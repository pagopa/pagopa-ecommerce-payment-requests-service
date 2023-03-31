package it.pagopa.ecommerce.payment.requests.clients

import it.pagopa.ecommerce.generated.transactions.model.CtQrCode
import it.pagopa.ecommerce.generated.transactions.model.ObjectFactory
import it.pagopa.ecommerce.generated.transactions.model.StOutcome
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.utils.client.ResponseSpecCustom
import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.function.Function
import java.util.function.Predicate


@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class NodeForPspClientTests {

  private lateinit var client: NodeForPspClient

  @Mock private lateinit var nodoWebClient: WebClient

  @Mock private lateinit var requestBodyUriSpec: RequestBodyUriSpec

  @Mock private lateinit var requestHeadersSpec: RequestHeadersSpec<*>

  @Mock private lateinit var responseSpec: ResponseSpec

  @Mock private lateinit var customResponseSpec: ResponseSpecCustom

  @BeforeEach
  fun init() {
    client = NodeForPspClient("", nodoWebClient)
  }

  @Test
  fun `should return verify payment response given valid payment notice`() = runTest {
    val objectFactory = ObjectFactory()
    val fiscalCode = "77777777777"
    val paymentNotice = "302000100000009424"
    val paymentDescription = "paymentDescription"
    val request = objectFactory.createVerifyPaymentNoticeReq()
    val qrCode = CtQrCode()
    qrCode.fiscalCode = fiscalCode
    qrCode.noticeNumber = paymentNotice
    request.qrCode = qrCode
    val response = objectFactory.createVerifyPaymentNoticeRes()
    response.outcome = StOutcome.OK
    response.fiscalCodePA = fiscalCode
    response.paymentDescription = paymentDescription
    val paymentList = objectFactory.createCtPaymentOptionsDescriptionList()
    response.paymentList = paymentList
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(SoapEnvelope::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.bodyToMono(VerifyPaymentNoticeRes::class.java))
      .willReturn(Mono.just(response))

    /** test */
    val testResponse =
      client.verifyPaymentNotice(objectFactory.createVerifyPaymentNoticeReq(request)).block()

    /** asserts */
    assertThat(testResponse?.fiscalCodePA).isEqualTo(fiscalCode)
    assertThat(testResponse?.paymentDescription).isEqualTo(paymentDescription)
    assertThat(testResponse?.outcome).isEqualTo(StOutcome.OK)
  }

  @Test
  fun `should return verify fault given duplicate payment notice`() = runTest {
    val objectFactory = ObjectFactory()
    val fiscalCode = "77777777777"
    val paymentNotice = "302000100000009424"
    val faultError = "PAA_PAGAMENTO_DUPLICATO"
    val request = objectFactory.createVerifyPaymentNoticeReq()
    val qrCode = CtQrCode()
    qrCode.fiscalCode = fiscalCode
    qrCode.noticeNumber = paymentNotice
    request.qrCode = qrCode
    val response = objectFactory.createVerifyPaymentNoticeRes()
    val ctFaultBean = objectFactory.createCtFaultBean()
    ctFaultBean.faultCode = faultError
    ctFaultBean.faultString = faultError
    response.fault = ctFaultBean

    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(SoapEnvelope::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.bodyToMono(VerifyPaymentNoticeRes::class.java))
      .willReturn(Mono.just(response))

    /** test */
    val testResponse =
      client.verifyPaymentNotice(objectFactory.createVerifyPaymentNoticeReq(request)).block()

    /** asserts */
    assertThat(testResponse?.fault?.faultCode).isEqualTo(faultError)
    assertThat(testResponse?.fault?.faultString).isEqualTo(faultError)
  }

  @Test
  fun `should return verify Rest api exception`() = runTest {
    val objectFactory = ObjectFactory()
    val fiscalCode = "77777777777"
    val paymentNotice = "302000100000009424"
    val faultError = "PAA_PAGAMENTO_DUPLICATO"
    val request = objectFactory.createVerifyPaymentNoticeReq()
    val qrCode = CtQrCode()
    qrCode.fiscalCode = fiscalCode
    qrCode.noticeNumber = paymentNotice
    request.qrCode = qrCode
    val response = objectFactory.createVerifyPaymentNoticeRes()
    val ctFaultBean = objectFactory.createCtFaultBean()
    ctFaultBean.faultCode = faultError
    ctFaultBean.faultString = faultError
    response.fault = ctFaultBean
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(SoapEnvelope::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(customResponseSpec)
    given(customResponseSpec.status).willReturn(HttpStatus.UNPROCESSABLE_ENTITY);
    given(
      customResponseSpec.onStatus(
        any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()
      )
    )
      .willCallRealMethod()

    assertThrows<RestApiException> { client.verifyPaymentNotice(objectFactory.createVerifyPaymentNoticeReq(request)) }
    /** test */
  }
}
