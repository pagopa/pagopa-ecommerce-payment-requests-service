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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

  @ParameterizedTest
  @EnumSource(PartyConfigurationFaultDto::class)
  fun `should return response entity with party configuration fault`(
    nodoErrorCode: PartyConfigurationFaultDto
  ) = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(nodoErrorCode.value)
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
        assertEquals(FaultCategoryDto.PAYMENT_UNAVAILABLE, it.faultCodeCategory)
        assertEquals(nodoErrorCode.value, it.faultCodeDetail.value)
      }
  }

  @ParameterizedTest
  @EnumSource(ValidationFaultDto::class)
  fun `should return response entity with validation fault`(nodoErrorCode: ValidationFaultDto) =
    runTest {
      val rptId = "77777777777302000100000009424"
      val faultBean = faultBeanWithCode(nodoErrorCode.value)
      given(paymentRequestsService.getPaymentRequestInfo(rptId))
        .willThrow(NodoErrorException(faultBean))
      val parameters = mapOf("rpt_id" to rptId)
      webClient
        .get()
        .uri("/payment-requests/{rpt_id}", parameters)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.NOT_FOUND)
        .expectBody(ValidationFaultPaymentProblemJsonDto::class.java)
        .value {
          assertEquals(FaultCategoryDto.PAYMENT_UNKNOWN, it.faultCodeCategory)
          assertEquals(nodoErrorCode.value, it.faultCodeDetail.value)
        }
    }

  @ParameterizedTest
  @EnumSource(GatewayFaultDto::class)
  fun `should return response entity with gateway fault`(nodoErrorCode: GatewayFaultDto) = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(nodoErrorCode.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.BAD_GATEWAY)
      .expectBody(GatewayFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(FaultCategoryDto.GENERIC_ERROR, it.faultCodeCategory)
        assertEquals(nodoErrorCode.value, it.faultCodeDetail.value)
      }
  }

  @ParameterizedTest
  @EnumSource(PartyTimeoutFaultDto::class)
  fun `should return response entity with party timeout fault`(
    nodoErrorCode: PartyTimeoutFaultDto
  ) = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(nodoErrorCode.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.GATEWAY_TIMEOUT)
      .expectBody(PartyTimeoutFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(FaultCategoryDto.GENERIC_ERROR, it.faultCodeCategory)
        assertEquals(nodoErrorCode.value, it.faultCodeDetail.value)
      }
  }

  @ParameterizedTest
  @EnumSource(PaymentStatusFaultDto::class)
  fun `should return response entity with payment status fault`(
    nodoErrorCode: PaymentStatusFaultDto
  ) = runTest {
    val rptId = "77777777777302000100000009424"
    val faultBean = faultBeanWithCode(nodoErrorCode.value)
    given(paymentRequestsService.getPaymentRequestInfo(rptId))
      .willThrow(NodoErrorException(faultBean))
    val parameters = mapOf("rpt_id" to rptId)
    webClient
      .get()
      .uri("/payment-requests/{rpt_id}", parameters)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
      .expectBody(PaymentStatusFaultPaymentProblemJsonDto::class.java)
      .value {
        assertEquals(FaultCategoryDto.PAYMENT_UNAVAILABLE, it.faultCodeCategory)
        assertEquals(nodoErrorCode.value, it.faultCodeDetail.value)
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
