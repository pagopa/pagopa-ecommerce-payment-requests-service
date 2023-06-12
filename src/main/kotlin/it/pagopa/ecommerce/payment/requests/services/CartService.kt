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
import java.net.URI
import java.text.MessageFormat
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class CartService(
  @Value("\${checkout.url}") private val checkoutUrl: String,
  @Autowired private val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper,
  @Autowired private val nodoPerPmClient: NodoPerPmClient,
  @Value("\${carts.max_allowed_payment_notices}") private val maxAllowedPaymentNotices: Int,
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
  fun processCart(cartRequestDto: CartRequestDto): Mono<String> {
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
        CartInfo(
          UUID.randomUUID(),
          paymentInfos,
          cartRequestDto.idCart,
          ReturnUrls(
            returnSuccessUrl = cartRequestDto.returnUrls.returnOkUrl.toString(),
            returnErrorUrl = cartRequestDto.returnUrls.returnErrorUrl.toString(),
            returnCancelUrl = cartRequestDto.returnUrls.returnCancelUrl.toString()),
          cartRequestDto.emailNotice)

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
        .map {
          logger.info("Saving cart ${cart.id} for payments $paymentInfos")

          cartsRedisTemplateWrapper.save(cart)
          val retUrl =
            MessageFormat.format(
              checkoutUrl,
              cart.id,
            )
          logger.info("Return URL: $retUrl")
          return@map retUrl
        }
    } else {
      logger.error("Too many payment notices, expected only one")
      Mono.error(
        RestApiException(
          httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
          title = "Multiple payment notices not processable",
          description = "Too many payment notices, expected max $maxAllowedPaymentNotices"))
    }
  }

  /*
   * Fetch the cart with the input cart id
   */
  fun getCart(cartId: UUID): CartRequestDto {
    val cart =
      cartsRedisTemplateWrapper.findById(cartId.toString())
        ?: throw CartNotFoundException(cartId.toString())

    return CartRequestDto().apply {
      paymentNotices =
        cart.payments.map {
          PaymentNoticeDto().apply {
            noticeNumber = it.rptId.noticeId
            fiscalCode = it.rptId.fiscalCode
            amount = it.amount
            companyName = it.companyName
            description = it.description
          }
        }
      returnUrls =
        cart.returnUrls.let {
          CartRequestReturnUrlsDto().apply {
            returnOkUrl = URI(it.returnSuccessUrl)
            returnCancelUrl = URI(it.returnCancelUrl)
            returnErrorUrl = URI(it.returnErrorUrl)
          }
        }
      emailNotice = cart.email
      idCart = cart.idCart
    }
  }
}
