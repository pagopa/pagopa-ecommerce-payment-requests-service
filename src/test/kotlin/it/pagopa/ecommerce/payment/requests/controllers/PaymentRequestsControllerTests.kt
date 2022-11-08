package it.pagopa.ecommerce.payment.requests.controllers


import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.payment.requests.server.model.*
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.services.PaymentRequestsService
import it.pagopa.ecommerce.payment.requests.tests.utils.PaymentRequests
import it.pagopa.ecommerce.payment.requests.validation.BeanValidationConfiguration
import it.pagopa.generated.nodoperpsp.model.FaultBean
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient


@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(PaymentRequestsController::class)
@Import(BeanValidationConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PaymentRequestsControllerTests {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    lateinit var paymentRequestsService: PaymentRequestsService

    @InjectMocks
    val paymentRequestController: PaymentRequestsController = PaymentRequestsController()

    companion object {
        fun faultBeanWithCode(faultCode: String): FaultBean {
            val faultBean = FaultBean()
            faultBean.faultCode = faultCode
            return faultBean;
        }

    }

    @Test
    fun `get payment request info`() = runTest {
        val objectMapper = ObjectMapper()
        val rptId = "77777777777302000100000009424"
        val response = PaymentRequests.validResponse(rptId)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willReturn(response)
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(objectMapper.writeValueAsString(response))
    }

    @Test
    fun `should return generic bad gateway response`() = runTest {
        val rptId = "77777777777302000100000009424"
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(RuntimeException::class.java)
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody(ProblemJsonDto::class.java).value {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), it.status)
            }
    }

    @Test
    fun `should return response entity with party configuration fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode(PartyConfigurationFaultDto.PPT_DOMINIO_DISABILITATO.value)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody(PartyConfigurationFaultPaymentProblemJsonDto::class.java).value {
                assertEquals(FaultCategoryDto.PAYMENT_UNAVAILABLE, it.faultCodeCategory)
                assertEquals(PartyConfigurationFaultDto.PPT_DOMINIO_DISABILITATO.value, it.faultCodeDetail.value)
            }
    }

    @Test
    fun `should return response entity with validation fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode(ValidationFaultDto.PPT_DOMINIO_SCONOSCIUTO.value)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody(ValidationFaultPaymentProblemJsonDto::class.java).value {
                assertEquals(FaultCategoryDto.PAYMENT_UNKNOWN, it.faultCodeCategory)
                assertEquals(ValidationFaultDto.PPT_DOMINIO_SCONOSCIUTO.value, it.faultCodeDetail.value)
            }
    }

    @Test
    fun `should return response entity with gateway fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode(GatewayFaultDto.PAA_SYSTEM_ERROR.value)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody(GatewayFaultPaymentProblemJsonDto::class.java).value {
                assertEquals(FaultCategoryDto.GENERIC_ERROR, it.faultCodeCategory)
                assertEquals(GatewayFaultDto.PAA_SYSTEM_ERROR.value, it.faultCodeDetail.value)
            }
    }

    @Test
    fun `should return response entity with party timeout fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode(PartyTimeoutFaultDto.PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE.value)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT)
            .expectBody(PartyTimeoutFaultPaymentProblemJsonDto::class.java).value {
                assertEquals(FaultCategoryDto.GENERIC_ERROR, it.faultCodeCategory)
                assertEquals(PartyTimeoutFaultDto.PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE.value, it.faultCodeDetail.value)
            }
    }

    @Test
    fun `should return response entity with payment status fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode(PaymentStatusFaultDto.PAA_PAGAMENTO_IN_CORSO.value)
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(PaymentStatusFaultPaymentProblemJsonDto::class.java).value {
                assertEquals(FaultCategoryDto.PAYMENT_UNAVAILABLE, it.faultCodeCategory)
                assertEquals(PaymentStatusFaultDto.PAA_PAGAMENTO_IN_CORSO.value, it.faultCodeDetail.value)
            }
    }


    @Test
    fun `should return response entity with generic gateway fault`() = runTest {
        val rptId = "77777777777302000100000009424"
        val faultBean = faultBeanWithCode("UNKNOWN_ERROR")
        given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(NodoErrorException(faultBean))
        val parameters = mapOf("rpt_id" to rptId)
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
    }
}
