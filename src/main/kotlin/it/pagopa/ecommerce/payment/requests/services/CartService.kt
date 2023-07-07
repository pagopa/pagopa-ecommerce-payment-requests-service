package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.ListelementRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.ConfidentialMailUtils
import java.net.URI
import java.text.MessageFormat
import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class CartService(
  @Value("\${checkout.url}") private val checkoutUrl: String,
  @Autowired private val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper,
  @Autowired private val nodoPerPmClient: NodoPerPmClient,
  @Autowired private val confidentialMailUtils: ConfidentialMailUtils,
  @Value("\${carts.max_allowed_payment_notices}") private val maxAllowedPaymentNotices: Int,
  private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  /*
   * Logger instance
   */
  var logger: Logger = LoggerFactory.getLogger(this.javaClass)

  /*
   * Process input cartRequestDto:
   * - 1 payment notice is present -> redirect response is given to the checkout location
   * - 2 or more payment notices are present -> error response
   */
  suspend fun processCart(cartRequestDto: CartRequestDto): String {
    val paymentsNotices = cartRequestDto.paymentNotices
    val receivedNotices = paymentsNotices.size
    logger.info("Received [$receivedNotices] payment notices")

    return if (receivedNotices <= maxAllowedPaymentNotices) {
      val paymentInfos =
        paymentsNotices.map {
          PaymentInfo(
            RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
        }

      val cart =
        confidentialMailUtils.toConfidential(cartRequestDto.emailNotice).map { tokenizedEmail ->
          CartInfo(
            UUID.randomUUID(),
            paymentInfos,
            cartRequestDto.idCart,
            ReturnUrls(
              returnSuccessUrl = cartRequestDto.returnUrls.returnOkUrl.toString(),
              returnErrorUrl = cartRequestDto.returnUrls.returnErrorUrl.toString(),
              returnCancelUrl = cartRequestDto.returnUrls.returnCancelUrl.toString()),
            tokenizedEmail.opaqueData)
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
        .filter { response -> response.outcome == CheckPositionResponseDto.OutcomeEnum.OK }
        .switchIfEmpty {
          throw RestApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Invalid payment info",
            description = "Invalid payment notice data")
        }
        .flatMap { cart }
        .map { validCart ->
          logger.info("Saving cart ${validCart.id} for payments $paymentInfos")

          cartsRedisTemplateWrapper.save(validCart)
          val retUrl =
            MessageFormat.format(
              checkoutUrl,
              validCart.id,
            )
          logger.info("Return URL: $retUrl")
          return@map retUrl
        }
        .awaitSingle()
    } else {
      logger.error("Too many payment notices, expected only one")
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Multiple payment notices not processable",
        description = "Too many payment notices, expected max $maxAllowedPaymentNotices")
    }
  }

  /*
   * Fetch the cart with the input cart id
   */
  suspend fun getCart(cartId: UUID): CartRequestDto {
    val cartWithTokenizedEmail =
      cartsRedisTemplateWrapper.findById(cartId.toString())
        ?: throw CartNotFoundException(cartId.toString())

    val cartWithClearEmail =
      confidentialMailUtils
        .toConfidential(cartWithTokenizedEmail.email)
        .flatMap { confidentialEmail -> confidentialMailUtils.toEmail(confidentialEmail) }
        .map { email ->
          CartRequestDto(
            paymentNotices =
              cartWithTokenizedEmail.payments.map {
                PaymentNoticeDto(
                  it.rptId.noticeId, it.rptId.fiscalCode, it.amount, it.companyName, it.description)
              },
            returnUrls =
              cartWithTokenizedEmail.returnUrls.let {
                CartRequestReturnUrlsDto(
                  returnOkUrl = URI(it.returnSuccessUrl),
                  returnCancelUrl = URI(it.returnCancelUrl),
                  returnErrorUrl = URI(it.returnErrorUrl))
              },
            emailNotice = email.value,
            idCart = cartWithTokenizedEmail.idCart)
        }
        .awaitSingle()

    return cartWithClearEmail
  }
}
