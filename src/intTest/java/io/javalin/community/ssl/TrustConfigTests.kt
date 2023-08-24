package io.javalin.community.ssl

import io.javalin.community.ssl.certs.Client
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okhttp3.tls.decodeCertificatePem
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession

@Tag("integration")
class TrustConfigTests : IntegrationTestClass() {
    @Test
    @DisplayName("Client with no certificate should not be able to access the server")
    fun unauthenticatedUserFails() {
        val unauthClient = client //This is the client without the client certificate
        val securePort = ports.getAndIncrement()
        val url = HTTPS_URL_WITH_PORT.apply(securePort)
        createTestApp { config: SSLConfig ->
            config.insecure = false
            config.securePort = securePort
            config.http2 = false // Disable HTTP/2 to avoid "connection closed" errors in tests due to connection reuse
            config.pemFromString(Client.SERVER_CERTIFICATE_AS_STRING, Client.SERVER_PRIVATE_KEY_AS_STRING)
            config.withTrustConfig { trustConfig: TrustConfig -> trustConfig.pemFromString(Client.CLIENT_CERTIFICATE_AS_STRING) }
        }
            .start().use { _ ->
                Assertions.assertThrows<Exception>(Exception::class.java) {
                    unauthClient.newCall(
                        Request.Builder().url(url).build()
                    ).execute()
                }
            }
    }

    @Test
    @DisplayName("Client with a wrong certificate should not be able to access the server")
    fun wrongCertificateFails() {
        val securePort = ports.getAndIncrement()
        val url = HTTPS_URL_WITH_PORT.apply(securePort)
        createTestApp { config: SSLConfig ->
            config.insecure = false
            config.securePort = securePort
            config.http2 = false // Disable HTTP/2 to avoid "connection closed" errors in tests due to connection reuse
            config.pemFromString(Client.SERVER_CERTIFICATE_AS_STRING, Client.SERVER_PRIVATE_KEY_AS_STRING)
            config.withTrustConfig { trustConfig: TrustConfig -> trustConfig.pemFromString(Client.CLIENT_CERTIFICATE_AS_STRING) }
        }.start().use { ignored ->
            //wrongClient.get().newCall(new Request.Builder().url(url).build()).execute();
            val req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build()
            Assertions.assertThrows(Exception::class.java) {
                wrongJavaClient.get()
                    .send(req, HttpResponse.BodyHandler<Any> { _: HttpResponse.ResponseInfo? -> null })
            }
        }
    }

    @Test
    @DisplayName("Loading PEM from a path works")
    fun pemFromPathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromPath(Client.CLIENT_PEM_PATH) }
    }

    @Test
    @DisplayName("Loading P7B from a path works")
    fun p7bFromPathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromPath(Client.CLIENT_P7B_PATH) }
    }

    @Test
    @DisplayName("Loading DER from a path works")
    fun derFromPathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromPath(Client.CLIENT_DER_PATH) }
    }

    @Test
    @DisplayName("Loading PEM from the classpath works")
    fun pemFromClasspathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromClasspath(Client.CLIENT_PEM_FILE_NAME) }
    }

    @Test
    @DisplayName("Loading P7B from the classpath works")
    fun p7bFromClasspathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromClasspath(Client.CLIENT_P7B_FILE_NAME) }
    }

    @Test
    @DisplayName("Loading DER from the classpath works")
    fun derFromClasspathWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromClasspath(Client.CLIENT_DER_FILE_NAME) }
    }

    @Test
    @DisplayName("Loading PEM from an input stream works")
    fun pemFromInputStreamWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromInputStream(Client.CLIENT_PEM_INPUT_STREAM_SUPPLIER.get()) }
    }

    @Test
    @DisplayName("Loading P7B from an input stream works")
    fun p7bFromInputStreamWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromInputStream(Client.CLIENT_P7B_INPUT_STREAM_SUPPLIER.get()) }
    }

    @Test
    @DisplayName("Loading DER from an input stream works")
    fun derFromInputStreamWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.certificateFromInputStream(Client.CLIENT_DER_INPUT_STREAM_SUPPLIER.get()) }
    }

    @Test
    @DisplayName("Loading PEM from a string works")
    fun pemFromStringWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.pemFromString(Client.CLIENT_CERTIFICATE_AS_STRING) }
    }

    @Test
    @DisplayName("Loading P7B from a string works")
    fun p7bFromStringWorks() {
        trustConfigWorks { trustConfig: TrustConfig -> trustConfig.p7bCertificateFromString(Client.CLIENT_P7B_CERTIFICATE_AS_STRING) }
    }

    @Test
    @DisplayName("Loading a JKS Keystore from a path works")
    fun jksFromPathWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromPath(
                Client.CLIENT_JKS_PATH,
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    @Test
    @DisplayName("Loading a P12 Keystore from a path works")
    fun p12FromPathWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromPath(
                Client.CLIENT_P12_PATH,
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    @Test
    @DisplayName("Loading a JKS Keystore from the classpath works")
    fun jksFromClasspathWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromClasspath(
                Client.CLIENT_JKS_FILE_NAME,
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    @Test
    @DisplayName("Loading a P12 Keystore from the classpath works")
    fun p12FromClasspathWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromClasspath(
                Client.CLIENT_P12_FILE_NAME,
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    @Test
    @DisplayName("Loading a JKS Keystore from an input stream works")
    fun jksFromInputStreamWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromInputStream(
                Client.CLIENT_JKS_INPUT_STREAM_SUPPLIER.get(),
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    @Test
    @DisplayName("Loading a P12 Keystore from an input stream works")
    fun p12FromInputStreamWorks() {
        trustConfigWorks { trustConfig: TrustConfig ->
            trustConfig.trustStoreFromInputStream(
                Client.CLIENT_P12_INPUT_STREAM_SUPPLIER.get(),
                Client.KEYSTORE_PASSWORD
            )
        }
    }

    companion object {
        private val authenticatedClient =
            Supplier { httpsClientBuilder(Client.CLIENT_CERTIFICATE_AS_STRING, Client.CLIENT_PRIVATE_KEY_AS_STRING) }
        private val wrongClient = Supplier {
            httpsClientBuilder(
                Client.WRONG_CLIENT_CERTIFICATE_AS_STRING,
                Client.WRONG_CLIENT_PRIVATE_KEY_AS_STRING
            )
        }
        private val wrongJavaClient = Supplier {
            javaHttpClientBuilder(
                Client.WRONG_CLIENT_CERTIFICATE_AS_STRING,
                Client.WRONG_CLIENT_PRIVATE_KEY_AS_STRING
            )
        }

        private fun httpsClientBuilder(clientCertificate: String, privateKey: String): OkHttpClient {
            val builder = HandshakeCertificates.Builder()
            //Server certificate
            builder.addTrustedCertificate(Client.SERVER_CERTIFICATE_AS_STRING.decodeCertificatePem())

            //Client certificate (Concatenated with the private key)
            val heldCertificate = HeldCertificate.decode(clientCertificate + privateKey)
            builder.heldCertificate(heldCertificate)
            val clientCertificates: HandshakeCertificates = builder.build()
            val sslContext: SSLContext
            try {
                sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, null)
                sslContext.init(
                    arrayOf(clientCertificates.keyManager), arrayOf(clientCertificates.trustManager),
                    null
                )
            } catch (e: KeyManagementException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            return ConnectionPool(0, 1, TimeUnit.MICROSECONDS).let {
                OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.socketFactory, clientCertificates.trustManager)
                    .hostnameVerifier{_,_ -> true}
                    .connectionPool(it)
                    .build()
            }
        }

        /**
         * Needed in order to test multiple wrong clients, see:
         * [Java HTTPS client certificate authentication](https://stackoverflow.com/a/32513368/5899345)
         * [
 * Java caching SSL failures - can I flush these somehow
](https://stackoverflow.com/questions/54671365/java-caching-ssl-failures-can-i-flush-these-somehow) *
         */
        private fun javaHttpClientBuilder(clientCertificate: String, privateKey: String): HttpClient {
            val builder = HandshakeCertificates.Builder()
            //Server certificate
            builder.addTrustedCertificate(Client.SERVER_CERTIFICATE_AS_STRING.decodeCertificatePem())

            //Client certificate (Concatenated with the private key)
            val heldCertificate = HeldCertificate.decode(clientCertificate + privateKey)
            builder.heldCertificate(heldCertificate)
            val clientCertificates: HandshakeCertificates = builder.build()
            val sslContext: SSLContext
            try {
                sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, null)
                sslContext.init(
                    arrayOf(clientCertificates.keyManager), arrayOf(clientCertificates.trustManager),
                    null
                )
            } catch (e: KeyManagementException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build()
        }

        @Throws(IOException::class)
        protected fun testSuccessfulEndpoint(url: String) {
            val client = authenticatedClient.get()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            Assertions.assertEquals(200, response.code)
            Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            response.close()
        }

        protected fun testWrongCertOnEndpoint(url: String?) {
            val req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build()
            Assertions.assertThrows(Exception::class.java) {
                wrongJavaClient.get().send(req, HttpResponse.BodyHandler<Any> { response: HttpResponse.ResponseInfo ->
                    println(response.statusCode())
                    null
                })
            }
        }

        protected fun trustConfigWorks(consumer: Consumer<TrustConfig>?) {
            val securePort = ports.getAndIncrement()
            val url = HTTPS_URL_WITH_PORT.apply(securePort)
            try {
                createTestApp { config: SSLConfig ->
                    config.insecure = false
                    config.securePort = securePort
                    config.pemFromString(Client.SERVER_CERTIFICATE_AS_STRING, Client.SERVER_PRIVATE_KEY_AS_STRING)
                    config.http2 = false
                    config.withTrustConfig(consumer)
                }.start().use { ignored ->
                    testSuccessfulEndpoint(url)
                    testWrongCertOnEndpoint(url)
                }
            } catch (e: Exception) {
                Assertions.fail<Any>(e)
            }
        }
    }
}
