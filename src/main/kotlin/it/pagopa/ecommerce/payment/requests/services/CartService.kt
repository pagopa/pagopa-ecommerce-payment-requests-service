package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.ListelementRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.ClientIdDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
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
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class CartService(
  @Value("\${checkout.url}") private val checkoutUrl: String,
  @Autowired private val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper,
  @Autowired private val nodoPerPmClient: NodoPerPmClient,
  @Autowired private val tokenizerMailUtils: TokenizerEmailUtils,
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
  suspend fun processCart(xClientId: ClientIdDto, cartRequestDto: CartRequestDto): String {
    val paymentsNotices = cartRequestDto.paymentNotices
    val receivedNotices = paymentsNotices.size
    logger.info("Received [$receivedNotices] payment notices")

    return if (receivedNotices <= maxAllowedPaymentNotices) {
      val paymentInfos =
        paymentsNotices.map {
          PaymentInfo(
            RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
        }

      if (receivedNotices != paymentInfos.map { it.rptId }.toSet().size) {
        logger.error("Duplicate payment notice values found for paymentNotices: $paymentsNotices")
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
        .filter { response -> response.outcome == CheckPositionResponseDto.OutcomeEnum.OK }
        .switchIfEmpty {
          throw RestApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Invalid payment info",
            description = "Invalid payment notice data")
        }
        .flatMap {
          val id = UUID.randomUUID()
          Optional.ofNullable(cartRequestDto.emailNotice)
            .map {
              tokenizerMailUtils.toConfidential(Email(cartRequestDto.emailNotice)).map {
                tokenizedEmail ->
                CartInfo(
                  id = id,
                  payments = paymentInfos,
                  idCart = cartRequestDto.idCart,
                  returnUrls =
                    ReturnUrls(
                      returnSuccessUrl = cartRequestDto.returnUrls.returnOkUrl.toString(),
                      returnErrorUrl = cartRequestDto.returnUrls.returnErrorUrl.toString(),
                      returnCancelUrl = cartRequestDto.returnUrls.returnCancelUrl.toString()),
                  email = tokenizedEmail.opaqueData)
              }
            }
            .orElse(
              Mono.just(
                CartInfo(
                  id = id,
                  payments = paymentInfos,
                  idCart = cartRequestDto.idCart,
                  returnUrls =
                    ReturnUrls(
                      returnSuccessUrl = cartRequestDto.returnUrls.returnOkUrl.toString(),
                      returnErrorUrl = cartRequestDto.returnUrls.returnErrorUrl.toString(),
                      returnCancelUrl = cartRequestDto.returnUrls.returnCancelUrl.toString()),
                  email = null)),
            )
        }
        .map { validCart ->
          logger.info("Saving cart ${validCart.id} for payments $paymentInfos")

          cartsRedisTemplateWrapper.save(validCart)
          val retUrl = MessageFormat.format(checkoutUrl, validCart.id, xClientId.value)
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

    return Optional.ofNullable(cartWithTokenizedEmail.email)
      .map { tokenizedMail ->
        tokenizerMailUtils.toEmail(Confidential(tokenizedMail)).map { clearMail ->
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
            emailNotice = clearMail.value,
            idCart = cartWithTokenizedEmail.idCart)
        }
      }
      .orElse(
        Mono.just(
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
            idCart = cartWithTokenizedEmail.idCart,
            emailNotice = null)))
      .awaitSingle()
  }
}
