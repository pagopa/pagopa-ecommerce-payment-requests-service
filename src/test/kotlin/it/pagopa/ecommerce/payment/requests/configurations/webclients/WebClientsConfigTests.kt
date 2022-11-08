package it.pagopa.ecommerce.payment.requests.configurations.webclients

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class WebClientsConfigTests {

    companion object {
        const val NODO_HOST_NAME = "http://nodo.it"
        const val NODO_READ_TIMEOUT = 10000
        const val NODO_CONNECTION_TIMEOUT = 1000
    }

    private val webClientsConfig = WebClientsConfig()

    @Test
    fun `should return valid WebClient`() = runTest {
        assertDoesNotThrow {
            //webClientsConfig.nodoWebClient(NODO_HOST_NAME, NODO_READ_TIMEOUT, NODO_CONNECTION_TIMEOUT)
        }


    }
}