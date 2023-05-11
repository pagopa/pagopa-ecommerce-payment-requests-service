package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.generated.transactions.model.CtQrCode
import it.pagopa.ecommerce.generated.transactions.model.StOutcome
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.configurations.nodo.NodoConfig
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.InvalidRptException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.PaymentRequestsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PaymentRequestsService(
  @Autowired private val paymentRequestInfoRepository: PaymentRequestsRedisTemplateWrapper,
  @Autowired private val nodeForPspClient: NodeForPspClient,
  @Autowired
  private val objectFactoryNodeForPsp:
    it.pagopa.ecommerce.generated.transactions.model.ObjectFactory,
  @Autowired private val nodoOperations: NodoOperations,
  @Autowired private val nodoConfig: NodoConfig
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
              .doOnSuccess { paymentRequestInfoRepository.save(it) }
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
    val paymentRequestInfoOptional: Optional<PaymentRequestInfo> =
      Optional.ofNullable(paymentRequestInfoRepository.findById(rptId.value))
    logger.info(
      "PaymentRequestInfo cache hit for {}: {}", rptId, paymentRequestInfoOptional.isPresent)
    return paymentRequestInfoOptional
      .filter { it.amount != null }
      .map { Mono.just(it) }
      .orElseGet { Mono.empty() }
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
                    getDueDateString(
                      verifyPaymentNoticeResponse.paymentList.paymentOptionDescription[0].dueDate),
                  paymentToken = null,
                  idempotencyKey = null,
                  transferList = null))
            }
          }
      return@flatMap paymentRequestInfo
    }

  fun isNodoError(verifyPaymentResponse: VerifyPaymentNoticeRes): Boolean {
    val outcome = verifyPaymentResponse.outcome
    return StOutcome.KO == outcome
  }

  fun getDueDateString(date: XMLGregorianCalendar?): String? = date?.toString()
}
