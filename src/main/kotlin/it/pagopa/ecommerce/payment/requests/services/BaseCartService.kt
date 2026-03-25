package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.*
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.*
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import java.text.MessageFormat
import java.util.*
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

data class CartRequest(
  val paymentNotices: List<PaymentNoticeData>,
  val returnOkUrl: String,
  val returnErrorUrl: String,
  val returnCancelUrl: String,
  val returnWaitingUrl: String?,
  val emailNotice: String?,
  val idCart: String?
)

data class PaymentNoticeData(
  val fiscalCode: String,
  val noticeNumber: String,
  val description: String,
  val amount: Int,
  val companyName: String
)

abstract class BaseCartService(
  protected val checkoutUrl: String,
  protected val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper,
  protected val nodoPerPmClient: NodoPerPmClient,
  protected val tokenizerMailUtils: TokenizerEmailUtils,
  protected val maxAllowedPaymentNotices: Int
) {

  protected fun buildCartRequest(
    paymentNotices: List<PaymentNoticeData>,
    returnOkUrl: String,
    returnErrorUrl: String,
    returnCancelUrl: String,
    returnWaitingUrl: String?,
    emailNotice: String?,
    idCart: String?
  ): CartRequest =
    CartRequest(
      paymentNotices = paymentNotices,
      returnOkUrl = returnOkUrl,
      returnErrorUrl = returnErrorUrl,
      returnCancelUrl = returnCancelUrl,
      returnWaitingUrl = returnWaitingUrl,
      emailNotice = emailNotice,
      idCart = idCart)

  protected val logger: Logger = LoggerFactory.getLogger(this.javaClass)

  protected abstract fun buildReturnUrls(request: CartRequest): ReturnUrls

  protected suspend fun processCartInternal(clientIdValue: String, request: CartRequest): String {
    val receivedNotices = request.paymentNotices.size
    logger.info("Received [$receivedNotices] payment notices")

    if (receivedNotices > maxAllowedPaymentNotices) {
      logger.error("Too many payment notices, expected only one")
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Multiple payment notices not processable",
        description = "Too many payment notices, expected max $maxAllowedPaymentNotices")
    }

    val paymentInfos =
      request.paymentNotices.map {
        PaymentInfo(
          RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
      }

    if (receivedNotices != paymentInfos.map { it.rptId }.toSet().size) {
      logger.error("Duplicate payment notice values found")
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Invalid payment info",
        description = "Duplicate payment notice values found.")
    }

    val checkPositionDto =
      CheckPositionDto()
        .positionslist(
          paymentInfos
            .stream()
            .map {
              ListelementRequestDto()
                .fiscalCode(it.rptId.fiscalCode)
                .noticeNumber(it.rptId.noticeId)
            }
            .toList())

    return nodoPerPmClient
      .checkPosition(checkPositionDto)
      .filter { it.outcome == CheckPositionResponseDto.OutcomeEnum.OK }
      .switchIfEmpty {
        throw RestApiException(
          httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
          title = "Invalid payment info",
          description = "Invalid payment notice data")
      }
      .flatMap {
        val id = UUID.randomUUID()
        val returnUrls = buildReturnUrls(request)

        Optional.ofNullable(request.emailNotice)
          .map {
            tokenizerMailUtils.toConfidential(Email(request.emailNotice)).map { tokenizedEmail ->
              CartInfo(id, paymentInfos, request.idCart, returnUrls, tokenizedEmail.opaqueData)
            }
          }
          .orElse(Mono.just(CartInfo(id, paymentInfos, request.idCart, returnUrls, null)))
      }
      .flatMap {
        logger.info("Saving cart ${it.id} for payments $paymentInfos")
        cartsRedisTemplateWrapper.save(it).thenReturn(it)
      }
      .map {
        val retUrl = MessageFormat.format(checkoutUrl, it.id, clientIdValue)
        logger.info("Return URL: $retUrl")
        retUrl
      }
      .awaitSingle()
  }
}
