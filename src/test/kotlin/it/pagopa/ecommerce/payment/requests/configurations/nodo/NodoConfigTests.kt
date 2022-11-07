package it.pagopa.ecommerce.payment.requests.configurations.nodo

import it.pagopa.ecommerce.payment.requests.configurations.webclients.WebClientsConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.annotation.Import

@ExtendWith(MockitoExtension::class)
@Import(WebClientsConfig::class)
class NodoConfigTests {
    companion object {
        const val NODO_CONNECTION_STRING =
            "{\"idPSP\":\"idPsp\",\"idChannel\":\"idChannel\",\"idBrokerPSP\":\"idBrokerPsp\",\"password\":\"password\"}"
    }

    @InjectMocks
    private lateinit var nodoConfig: NodoConfig

    @Test
    fun `should return valid VerificaRPTBaseRequest`() = runTest {
        val nodoVerificaRPT = nodoConfig.baseNodoVerificaRPTRequest(NODO_CONNECTION_STRING)
        assertEquals(nodoVerificaRPT.identificativoPSP, "idPsp")
        assertEquals(nodoVerificaRPT.identificativoCanale, "idChannel")
        assertEquals(nodoVerificaRPT.identificativoIntermediarioPSP, "idBrokerPsp")
        assertEquals(nodoVerificaRPT.password, "password")
        assertEquals(nodoVerificaRPT.codificaInfrastrutturaPSP, "QR-CODE")
    }

    @Test
    fun `should return valid VerifyPaymentNoticeRq`() = runTest {
        val nodoVerificaRPT = nodoConfig.baseVerifyPaymentNoticeReq(NODO_CONNECTION_STRING)
        assertEquals(nodoVerificaRPT.idPSP, "idPsp")
        assertEquals(nodoVerificaRPT.idChannel, "idChannel")
        assertEquals(nodoVerificaRPT.idBrokerPSP, "idBrokerPsp")
        assertEquals(nodoVerificaRPT.password, "password")
    }
}