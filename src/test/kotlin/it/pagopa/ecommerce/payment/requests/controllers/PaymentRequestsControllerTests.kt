package it.pagopa.ecommerce.payment.requests.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.payment.requests.server.model.*
import it.pagopa.ecommerce.generated.transactions.model.CtFaultBean
import it.pagopa.ecommerce.payment.requests.exceptions.InvalidRptException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.services.PaymentRequestsService
import it.pagopa.ecommerce.payment.requests.tests.utils.PaymentRequests
import it.pagopa.ecommerce.payment.requests.validation.BeanValidationConfiguration
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(PaymentRequestsController::class)
@Import(BeanValidationConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PaymentRequestsControllerTests {
  @Autowired lateinit var webClient: WebTestClient

  @MockBean lateinit var paymentRequestsService: PaymentRequestsService

  @Mock private lateinit var requestHeadersSpec: WebClient.RequestHeadersUriSpec<*>

  @Mock private lateinit var responseSpec: WebClient.ResponseSpec

  @InjectMocks
  private val paymentRequestController: PaymentRequestsController = PaymentRequestsController()

  companion object {
    fun faultBeanWithCode(faultCode: String): CtFaultBean {
      val faultBean = CtFaultBean()
      faultBean.faultCode = faultCode
      return faultBean
    }
  }

  @Test
  fun `get payment request info`() = runTest {
    val objectMapper = ObjectMapper()
    val rptId = "77777777777302000100000009424"
    val response = PaymentRequests.validResponse(rptId)
    given(paymentRequestsService.getPaymentRequestInfo(rptId)).willReturn(response)
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(objectMapper.writeValueAsString(response))
  }

  @Test
  fun `should return generic bad gateway response`() = runTest {
    val rptId = "77777777777302000100000009424"
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(RuntimeException::class.java)
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      .expectBody(ProblemJsonDto::class.java)
      .value { assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), it.status) }
  }

  @Test
  fun `should return bad request response`() = runTest {
    val rptId = "invalidRPT"
    given(paymentRequestsService.getPaymentRequestInfo(rptId)).willThrow(InvalidRptException(rptId))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_REQUEST)
      .expectBody(ProblemJsonDto::class.java)
      .value { assertEquals(HttpStatus.BAD_REQUEST.value(), it.status) }
  }

  @Test
  fun `should return response entity with party configuration fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(PartyConfigurationFaultDto.PAA_ID_DOMINIO_ERRATO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
      .expectBody(PartyConfigurationFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(
          PartyConfigurationFaultPaymentProblemJsonDto.FaultCodeCategory.DOMAIN_UNKNOWN,
          it.faultCodeCategory)
        assertEquals(
          PartyConfigurationFaultDto.PAA_ID_DOMINIO_ERRATO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with validation unknown fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean =
      faultBeanWithCode(ValidationFaultPaymentUnknownDto.PPT_DOMINIO_SCONOSCIUTO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.NOT_FOUND)
      .expectBody(ValidationFaultPaymentUnknownProblemJsonDto::class.java)
      .value {
        assertEquals(
          ValidationFaultPaymentUnknownProblemJsonDto.FaultCodeCategory.PAYMENT_UNKNOWN,
          it.faultCodeCategory)
        assertEquals(
          ValidationFaultPaymentUnknownDto.PPT_DOMINIO_SCONOSCIUTO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with validation unavailable fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean =
      faultBeanWithCode(ValidationFaultPaymentUnavailableDto.PPT_CANALE_DISABILITATO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(ValidationFaultPaymentUnavailableProblemJsonDto::class.java)
      .value {
        assertEquals(
          ValidationFaultPaymentUnavailableProblemJsonDto.FaultCodeCategory.PAYMENT_UNAVAILABLE,
          it.faultCodeCategory)
        assertEquals(
          ValidationFaultPaymentUnavailableDto.PPT_CANALE_DISABILITATO.value,
          it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with ongoing payment status fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(PaymentOngoingStatusFaultDto.PAA_PAGAMENTO_IN_CORSO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody(PaymentOngoingStatusFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(
          PaymentOngoingStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_ONGOING,
          it.faultCodeCategory)
        assertEquals(
          PaymentOngoingStatusFaultDto.PAA_PAGAMENTO_IN_CORSO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with expired payment status fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(PaymentExpiredStatusFaultDto.PAA_PAGAMENTO_SCADUTO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody(PaymentExpiredStatusFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(
          PaymentExpiredStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_EXPIRED,
          it.faultCodeCategory)
        assertEquals(
          PaymentExpiredStatusFaultDto.PAA_PAGAMENTO_SCADUTO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with canceled payment status fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(PaymentCanceledStatusFaultDto.PAA_PAGAMENTO_ANNULLATO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody(PaymentCanceledStatusFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(
          PaymentCanceledStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_CANCELED,
          it.faultCodeCategory)
        assertEquals(
          PaymentCanceledStatusFaultDto.PAA_PAGAMENTO_ANNULLATO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with duplicated payment status fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(PaymentDuplicatedStatusFaultDto.PAA_PAGAMENTO_DUPLICATO.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody(PaymentDuplicatedStatusFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(
          PaymentDuplicatedStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_DUPLICATED,
          it.faultCodeCategory)
        assertEquals(
          PaymentDuplicatedStatusFaultDto.PAA_PAGAMENTO_DUPLICATO.value, it.faultCodeDetail.value)
      }
  }

  @Test
  fun `should return response entity with generic gateway fault`() = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode("UNKNOWN_ERROR")
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_GATEWAY)
  }

  @Test
  fun `warm up controller`() {
    val webClient = mock(WebClient::class.java)
    given(webClient.get()).willReturn(requestHeadersSpec)
    given(requestHeadersSpec.uri(any(), any<Map<String, String>>())).willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.toBodilessEntity()).willReturn(Mono.empty())
    PaymentRequestsController(webClient).warmupGetPaymentRequest()
    verify(webClient, times(1)).get()
  }
}
