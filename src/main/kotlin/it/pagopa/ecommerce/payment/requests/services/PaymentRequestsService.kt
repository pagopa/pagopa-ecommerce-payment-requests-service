package it.pagopa.ecommerce.payment.requests.services

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.generated.transactions.model.CtQrCode
import it.pagopa.ecommerce.generated.transactions.model.StOutcome
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.configurations.nodo.NodoConfig
import it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.util.OpenTelemetryUtils
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.InvalidRptException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.ReactivePaymentRequestsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

private const val VERIFY_PAYMENT_NOTICE_NODO_ERROR_SPAN_NAME =
  "VerifyPaymentNotice nodo error: [%s]"
private const val VERIFY_PAYMENT_NOTICE_NODO_OK_SPAN_NAME = "VerifyPaymentNotice nodo ok"
private const val FAULT_CODE_SPAN_KEY = "faultCode"

@Service
class PaymentRequestsService(
  @Autowired private val paymentRequestInfoRepository: ReactivePaymentRequestsRedisTemplateWrapper,
  @Autowired private val nodeForPspClient: NodeForPspClient,
  @Autowired
  private val objectFactoryNodeForPsp:
    it.pagopa.ecommerce.generated.transactions.model.ObjectFactory,
  @Autowired private val nodoOperations: NodoOperations,
  @Autowired private val nodoConfig: NodoConfig,
  @Autowired private val openTelemetryUtils: OpenTelemetryUtils
) {

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  suspend fun getPaymentRequestInfo(rptId: String): PaymentRequestsGetResponseDto {
    val rptIdRecord: RptId
    try {
      rptIdRecord = RptId(rptId)
    } catch (e: IllegalArgumentException) {
      throw InvalidRptException(rptId)
    }
    val paymentContextCode = UUID.randomUUID().toString().replace("-", "")
    val paymentInfo =
      getPaymentInfoFromCache(rptIdRecord)
        .switchIfEmpty(
          Mono.defer {
            getPaymentInfoFromNodo(rptIdRecord, paymentContextCode)
              .doOnNext {
                logger.info(
                  "PaymentRequestInfo from nodo pagoPA for {}",
                  rptId,
                )
              }
              .doOnSuccess { paymentRequestInfoRepository.save(it).thenReturn { Mono.just(it) } }
          })
        .map { paymentInfo ->
          PaymentRequestsGetResponseDto(
            rptId = paymentInfo.id.value,
            paFiscalCode = paymentInfo.paFiscalCode,
            paName = paymentInfo.paName,
            dueDate = paymentInfo.dueDate,
            description = paymentInfo.description,
            amount = paymentInfo.amount!!,
            paymentContextCode = paymentContextCode)
        }
        .doOnNext { logger.info("PaymentRequestInfo retrieved for {}", rptId) }
    return paymentInfo.awaitSingle()
  }

  suspend fun getPaymentInfoFromCache(rptId: RptId): Mono<PaymentRequestInfo> {
    return paymentRequestInfoRepository.findById(rptId.value).filter { it.amount != null }
  }

  fun getPaymentInfoFromNodo(rptId: RptId, paymentContextCode: String): Mono<PaymentRequestInfo> =
    Mono.just(rptId).flatMap {
      logger.info(
        "Calling Nodo for VerifyPaymentNotice for get payment info for rptId: [{}]. PaymentContextCode: [{}]",
        rptId.value,
        paymentContextCode)

      val verifyPaymentNoticeReq = nodoConfig.baseVerifyPaymentNoticeReq()
      val qrCode = CtQrCode()
      qrCode.fiscalCode = rptId.fiscalCode
      qrCode.noticeNumber = rptId.noticeId
      verifyPaymentNoticeReq.qrCode = qrCode
      val paymentRequestInfo: Mono<PaymentRequestInfo> =
        nodeForPspClient
          .verifyPaymentNotice(
            objectFactoryNodeForPsp.createVerifyPaymentNoticeReq(verifyPaymentNoticeReq))
          .flatMap { verifyPaymentNoticeResponse ->
            val isNodoError = isNodoError(verifyPaymentNoticeResponse)
            logger.info(
              "VerifyPaymentNotice: outcome: [{}] fault code: [{}] is nodo error: [{}]",
              verifyPaymentNoticeResponse.outcome,
              verifyPaymentNoticeResponse?.fault?.faultCode,
              isNodoError)
            traceVerifyNodoOutcome(verifyPaymentNoticeResponse?.fault?.faultCode, isNodoError)
            if (isNodoError) {
              Mono.error(NodoErrorException(verifyPaymentNoticeResponse.fault))
            } else {
              Mono.just(
                PaymentRequestInfo(
                  id = rptId,
                  paFiscalCode = verifyPaymentNoticeResponse.fiscalCodePA,
                  paName = verifyPaymentNoticeResponse.companyName,
                  description = verifyPaymentNoticeResponse.paymentDescription,
                  amount =
                    nodoOperations.getEuroCentsFromNodoAmount(
                      verifyPaymentNoticeResponse.paymentList.paymentOptionDescription[0].amount),
                  dueDate =
                    dueDateToLocalDate(
                      verifyPaymentNoticeResponse.paymentList.paymentOptionDescription[0].dueDate),
                  paymentToken = null,
                  idempotencyKey = null,
                  activationDate = null,
                  transferList = null,
                  isAllCCP = null,
                  creditorReferenceId = null))
            }
          }
      return@flatMap paymentRequestInfo
    }

  private fun traceVerifyNodoOutcome(faultCode: String?, isNodoError: Boolean) {
    val nodoFaultCode = Optional.ofNullable(faultCode).orElse("No faultCode received")
    if (isNodoError) {
      openTelemetryUtils.addErrorSpanWithAttributes(
        VERIFY_PAYMENT_NOTICE_NODO_ERROR_SPAN_NAME.format(nodoFaultCode),
        Attributes.of(AttributeKey.stringKey(FAULT_CODE_SPAN_KEY), nodoFaultCode))
    } else {
      openTelemetryUtils.addSpanWithAttributes(
        VERIFY_PAYMENT_NOTICE_NODO_OK_SPAN_NAME,
        Attributes.of(AttributeKey.stringKey(FAULT_CODE_SPAN_KEY), StOutcome.OK.toString()))
    }
  }

  fun isNodoError(verifyPaymentResponse: VerifyPaymentNoticeRes): Boolean {
    val outcome = verifyPaymentResponse.outcome
    return StOutcome.KO == outcome
  }

  private fun dueDateToLocalDate(date: XMLGregorianCalendar?): String? =
    date?.let {
      LocalDate.ofInstant(date.toGregorianCalendar().toInstant(), ZoneId.of("Europe/Paris"))
        .toString()
    }
}
