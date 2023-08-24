package io.javalin.community.ssl

import io.javalin.community.ssl.certs.Server
import nl.altindag.ssl.exception.GenericIOException
import nl.altindag.ssl.exception.GenericKeyStoreException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.function.Supplier

@Tag("integration")
class KeystoreLoadingTests : IntegrationTestClass() {
    //////////////////////////////
    // Valid keystore loading   //
    //////////////////////////////
    @Test
    fun `loading a valid JKS keystore from the classpath`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromClasspath(
                Server.P12_KEY_STORE_NAME,
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    @Test
    fun `loading a valid P12 keystore from the classpath`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromClasspath(
                Server.P12_KEY_STORE_NAME,
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    @Test
    fun `loading a valid JKS keystore from a path`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromPath(
                Server.P12_KEY_STORE_PATH,
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    @Test
    fun `loading a valid P12 keystore from a path`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromPath(
                Server.P12_KEY_STORE_PATH,
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    @Test
    fun `loading a valid JKS keystore from an input stream`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromInputStream(
                Server.JKS_KEY_STORE_INPUT_STREAM_SUPPLIER.get(),
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    @Test
    fun `loading a valid P12 keystore from an input stream`() {
        assertSslWorks { config: SSLConfig ->
            config.keystoreFromInputStream(
                Server.P12_KEY_STORE_INPUT_STREAM_SUPPLIER.get(),
                Server.KEY_STORE_PASSWORD
            )
        }
    }

    //////////////////////////////
    // Invalid keystore loading //
    //////////////////////////////
    @Test
    fun `loading a missing JKS keystore from the classpath fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromClasspath(
                    "invalid",
                    Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a JKS keystore from the classpath with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromClasspath(
                    Server.JKS_KEY_STORE_NAME, "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a P12 keystore from the classpath with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromClasspath(
                    Server.P12_KEY_STORE_NAME, "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a missing JKS keystore from a path fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromPath(
                    "invalid",
                    Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a JKS keystore from a path with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromPath(
                    Server.JKS_KEY_STORE_PATH, "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a P12 keystore from a path with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromPath(
                    Server.P12_KEY_STORE_PATH, "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a missing JKS keystore from an input stream fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromInputStream(
                    InputStream.nullInputStream(), Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a JKS keystore from an input stream with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromInputStream(
                    Server.JKS_KEY_STORE_INPUT_STREAM_SUPPLIER.get(), "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a P12 keystore from an input stream with an invalid password fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromInputStream(
                    Server.P12_KEY_STORE_INPUT_STREAM_SUPPLIER.get(), "invalid"
                )
            }
        }
    }

    @Test
    fun `loading a malformed JKS keystore from the classpath fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromClasspath(
                    MALFORMED_JKS_FILE_NAME, Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a malformed P12 keystore from the classpath fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromClasspath(
                    MALFORMED_P12_FILE_NAME, Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a malformed JKS keystore from a path fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromPath(
                    MALFORMED_JKS_FILE_PATH, Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a malformed P12 keystore from a path fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromPath(
                    MALFORMED_P12_FILE_PATH, Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    @DisplayName("Loading a malformed JKS keystore from an input stream fails")
    fun loadMalformedJKSFromInputStream() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromInputStream(
                    MALFORMED_JKS_INPUT_STREAM_SUPPLIER.get(), Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    @Test
    fun `loading a malformed P12 keystore from an input stream fails`() {
        Assertions.assertThrows(GenericKeyStoreException::class.java) {
            assertSslWorks { config: SSLConfig ->
                config.keystoreFromInputStream(
                    MALFORMED_P12_INPUT_STREAM_SUPPLIER.get(), Server.KEY_STORE_PASSWORD
                )
            }
        }
    }

    companion object {
        private const val MALFORMED_JKS_FILE_NAME = "server/malformed.jks"
        private const val MALFORMED_P12_FILE_NAME = "server/malformed.p12"
        private val MALFORMED_JKS_FILE_PATH: String
        private val MALFORMED_P12_FILE_PATH: String

        init {
            try {
                MALFORMED_JKS_FILE_PATH =
                    Path.of(ClassLoader.getSystemResource(MALFORMED_JKS_FILE_NAME).toURI()).toAbsolutePath().toString()
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

        init {
            try {
                MALFORMED_P12_FILE_PATH =
                    Path.of(ClassLoader.getSystemResource(MALFORMED_P12_FILE_NAME).toURI()).toAbsolutePath().toString()
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

        val MALFORMED_JKS_INPUT_STREAM_SUPPLIER = Supplier {
            try {
                return@Supplier KeystoreLoadingTests::class.java.getResourceAsStream(MALFORMED_JKS_FILE_NAME)
            } catch (e: Exception) {
                throw GenericIOException(e)
            }
        }
        val MALFORMED_P12_INPUT_STREAM_SUPPLIER = Supplier {
            try {
                return@Supplier KeystoreLoadingTests::class.java.getResourceAsStream(MALFORMED_JKS_FILE_NAME)
            } catch (e: Exception) {
                throw GenericIOException(e)
            }
        }
    }
}
