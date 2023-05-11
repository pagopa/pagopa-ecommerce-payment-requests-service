package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.transactions.model.*
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.configurations.nodo.NodoConfig
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.InvalidRptException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.PaymentRequestsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.datatype.DatatypeFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class PaymentRequestsServiceTests {

  private lateinit var paymentRequestsService: PaymentRequestsService

  @Mock
  private lateinit var paymentRequestsRedisTemplateWrapper: PaymentRequestsRedisTemplateWrapper

  @Mock private lateinit var nodeForPspClient: NodeForPspClient

  @Mock private lateinit var nodoOperations: NodoOperations

  @Mock private lateinit var nodoConfig: NodoConfig

  @BeforeEach
  fun init() {
    paymentRequestsService =
      PaymentRequestsService(
        paymentRequestsRedisTemplateWrapper,
        nodeForPspClient,
        ObjectFactory(),
        nodoOperations,
        nodoConfig)
  }

  @Test
  fun `should return payment info from cache`() = runTest {
    val rptIdAsString = "77777777777302016723749670035"
    val rptIdAsObject = RptId(rptIdAsString)
    val paTaxCode = "77777777777"
    val paName = "Pa Name"
    val description = "Payment request description"
    val amount = Integer.valueOf(1000)
    val paymentRequestInfo =
      PaymentRequestInfo(
        rptIdAsObject, paTaxCode, paName, description, amount, null, null, null, null)
    /** preconditions */
    given(paymentRequestsRedisTemplateWrapper.getValue(rptIdAsString))
      .willReturn(paymentRequestInfo)
    /** test */
    val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)

    /** assertions */
    assertEquals(rptIdAsString, responseDto.rptId)
    assertEquals(description, responseDto.description)
    assertNull(responseDto.dueDate)
    assertEquals(amount, responseDto.amount)
    assertEquals(paName, responseDto.paName)
    assertEquals(paTaxCode, responseDto.paFiscalCode)
  }

  @Test
  fun `should return payment request info from Nodo VerifyPaymentNotice`() = runTest {
    val rptIdAsString = "77777777777302016723749670035"
    val description = "Payment request description"
    val amount = Integer.valueOf(1000)
    val amountForNodo = BigDecimal.valueOf(amount.toLong())

    val verifyPaymentNotice = VerifyPaymentNoticeRes()
    verifyPaymentNotice.outcome = StOutcome.OK
    verifyPaymentNotice.paymentDescription = description
    val paymentList = CtPaymentOptionsDescriptionList()
    val paymentDescription = CtPaymentOptionDescription()
    paymentDescription.amount = amountForNodo
    paymentList.paymentOptionDescription.add(paymentDescription)
    verifyPaymentNotice.paymentList = paymentList

    /** preconditions */
    given(nodoConfig.baseVerifyPaymentNoticeReq()).willReturn(VerifyPaymentNoticeReq())
    given(paymentRequestsRedisTemplateWrapper.getValue(rptIdAsString)).willReturn(null)
    given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
    given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

    /** test */
    val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
    /** assertions */
    assertEquals(rptIdAsString, responseDto.rptId)
    assertEquals(description, responseDto.description)
    assertNull(responseDto.dueDate)
    assertEquals(amount, responseDto.amount)
  }

  @Test
  fun `should return payment info request from nodo with VerifyPaymentNotice with due date`() =
    runTest {
      val rptIdAsString = "77777777777302016723749670035"
      val description = "Payment request description"
      val amount = 1000
      val amountForNodo = BigDecimal.valueOf(amount.toLong())

      val format: DateFormat = SimpleDateFormat("yyyy-MM-dd")
      val date = format.parse("2022-04-24")
      val dueDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(format.format(date))

      val verifyPaymentNotice = VerifyPaymentNoticeRes()
      verifyPaymentNotice.outcome = StOutcome.OK
      verifyPaymentNotice.paymentDescription = description
      val paymentList = CtPaymentOptionsDescriptionList()
      val paymentDescription = CtPaymentOptionDescription()
      paymentDescription.amount = amountForNodo
      paymentDescription.dueDate = dueDate
      paymentList.paymentOptionDescription.add(paymentDescription)
      verifyPaymentNotice.paymentList = paymentList

      /** preconditions */
      given(nodoConfig.baseVerifyPaymentNoticeReq()).willReturn(VerifyPaymentNoticeReq())
      given(paymentRequestsRedisTemplateWrapper.getValue(rptIdAsString)).willReturn(null)
      given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
      given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

      /** test */
      val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
      /** assertions */
      assertEquals(rptIdAsString, responseDto.rptId)
      assertEquals(description, responseDto.description)
      assertEquals(responseDto.dueDate, "2022-04-24")
      assertEquals(amount, responseDto.amount)
    }

  @Test
  fun `should return invalid request for invalid rpt id`() = runTest {
    val rptIdAsString = "invalid rpt id"
    val exception =
      assertThrows<InvalidRptException> {
        paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
      }
    assertEquals(HttpStatus.BAD_REQUEST, exception.toRestException().httpStatus)
    assertTrue(exception.toRestException().description.contains(rptIdAsString))
  }

  @Test
  fun `should return nodo error from VerifyPaymentNotice KO response with fault bean`() = runTest {
    val rptIdAsString = "77777777777302016723749670035"

    val verifyPaymentNotice = VerifyPaymentNoticeRes()
    verifyPaymentNotice.outcome = StOutcome.KO
    verifyPaymentNotice.fault = CtFaultBean()
    verifyPaymentNotice.fault.faultCode = "PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE"

    /** preconditions */
    given(paymentRequestsRedisTemplateWrapper.getValue(rptIdAsString)).willReturn(null)
    given(nodoConfig.baseVerifyPaymentNoticeReq()).willReturn(VerifyPaymentNoticeReq())
    given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
    /** test */
    val exception =
      assertThrows<NodoErrorException> {
        paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
      }
    /** assertions */
    assertEquals("PPT_STAZIONE_INT_PA_IRRAGGIUNGIBILE", exception.faultCode)
  }

  @Test
  fun `should return nodo error from VerifyPaymentNotice KO response no fault bean`() = runTest {
    val rptIdAsString = "77777777777302016723749670035"

    val verifyPaymentNotice = VerifyPaymentNoticeRes()
    verifyPaymentNotice.outcome = StOutcome.KO

    /** preconditions */
    given(paymentRequestsRedisTemplateWrapper.getValue(rptIdAsString)).willReturn(null)
    given(nodoConfig.baseVerifyPaymentNoticeReq()).willReturn(VerifyPaymentNoticeReq())
    given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
    /** test */
    val exception =
      assertThrows<NodoErrorException> {
        paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
      }
    /** assertions */
    assertEquals("Unreadable fault code", exception.faultCode)
  }
}
