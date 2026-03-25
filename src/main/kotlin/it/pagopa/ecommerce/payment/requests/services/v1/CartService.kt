package it.pagopa.ecommerce.payment.requests.services.v1

import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.ClientIdDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.services.BaseCartService
import it.pagopa.ecommerce.payment.requests.services.CartRequest
import it.pagopa.ecommerce.payment.requests.services.PaymentNoticeData
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service("CartServiceV1")
class CartService(
  @Value("\${checkout.url}") checkoutUrl: String,
  @Autowired cartsRedisTemplateWrapper: CartsRedisTemplateWrapper,
  @Autowired nodoPerPmClient: NodoPerPmClient,
  @Autowired tokenizerMailUtils: TokenizerEmailUtils,
  @Value("\${carts.max_allowed_payment_notices}") maxAllowedPaymentNotices: Int,
  private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) :
  BaseCartService(
    checkoutUrl,
    cartsRedisTemplateWrapper,
    nodoPerPmClient,
    tokenizerMailUtils,
    maxAllowedPaymentNotices) {

  override fun buildReturnUrls(request: CartRequest): ReturnUrls =
    ReturnUrls(
      returnSuccessUrl = request.returnOkUrl,
      returnErrorUrl = request.returnErrorUrl,
      returnCancelUrl = request.returnCancelUrl,
      returnWaitingUrl = null)
  /*
   * Process input cartRequestDto:
   * - 1 payment notice is present -> redirect response is given to the checkout location
   * - 2 or more payment notices are present -> error response
   */
  suspend fun processCart(xClientId: ClientIdDto, dto: CartRequestDto): String =
    processCartInternal(
      clientIdValue = xClientId.value,
      request =
        buildCartRequest(
          paymentNotices =
            dto.paymentNotices.map {
              PaymentNoticeData(
                fiscalCode = it.fiscalCode,
                noticeNumber = it.noticeNumber,
                description = it.description,
                amount = it.amount,
                companyName = it.companyName)
            },
          returnOkUrl = dto.returnUrls.returnOkUrl.toString(),
          returnErrorUrl = dto.returnUrls.returnErrorUrl.toString(),
          returnCancelUrl = dto.returnUrls.returnCancelUrl.toString(),
          returnWaitingUrl = null,
          emailNotice = dto.emailNotice,
          idCart = dto.idCart))
  /*
   * Fetch the cart with the input cart id
   */
  suspend fun getCart(cartId: UUID): CartRequestDto {
    return cartsRedisTemplateWrapper
      .findById(cartId.toString())
      .switchIfEmpty { throw CartNotFoundException(cartId.toString()) }
      .flatMap { cartWithTokenizedEmail ->
        val paymentNotices =
          cartWithTokenizedEmail.payments.map {
            PaymentNoticeDto(
              it.rptId.noticeId, it.rptId.fiscalCode, it.amount, it.companyName, it.description)
          }

        val returnUrls =
          cartWithTokenizedEmail.returnUrls.let {
            CartRequestReturnUrlsDto(
              returnOkUrl = URI(it.returnSuccessUrl),
              returnCancelUrl = URI(it.returnCancelUrl),
              returnErrorUrl = URI(it.returnErrorUrl),
              returnWaitingUrl = it.returnWaitingUrl?.let(::URI))
          }

        val idCart = cartWithTokenizedEmail.idCart

        Mono.justOrEmpty(cartWithTokenizedEmail.email)
          .flatMap { tokenizedMail ->
            tokenizerMailUtils.toEmail(Confidential(tokenizedMail)).map { clearMail ->
              CartRequestDto(
                paymentNotices = paymentNotices,
                returnUrls = returnUrls,
                emailNotice = clearMail.value,
                idCart = idCart)
            }
          }
          .defaultIfEmpty(
            CartRequestDto(
              paymentNotices = paymentNotices,
              returnUrls = returnUrls,
              emailNotice = null,
              idCart = idCart))
      }
      .awaitSingle()
  }
}
