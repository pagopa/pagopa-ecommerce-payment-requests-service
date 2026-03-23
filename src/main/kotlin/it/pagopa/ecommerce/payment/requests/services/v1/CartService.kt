package it.pagopa.ecommerce.payment.requests.services.v1

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.ListelementRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.ClientIdDto
import it.pagopa.ecommerce.generated.payment.requests.server.v1.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.v1.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.repositories.v1.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.v1.ReturnUrls
import it.pagopa.ecommerce.payment.requests.services.CartServiceHelper
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service("CartServiceV1")
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
  suspend fun processCart(clientId: ClientIdDto, cartRequestDto: CartRequestDto): String {
    return CartServiceHelper.processCart(
      xClientId = clientId,
      cartRequestDto = cartRequestDto,
      maxAllowedPaymentNotices = maxAllowedPaymentNotices,
      checkoutUrl = checkoutUrl,
      tokenizerMailUtils = tokenizerMailUtils,
      saveCart = { cartsRedisTemplateWrapper.save(it).thenReturn(it).awaitSingle() },
      mapToCartInfo = { id, payments, idCart, returnUrls, email ->
        CartInfo(id, payments, idCart, returnUrls, email)
      },
      extractPaymentNotices = { dto ->
        dto.paymentNotices.map { p ->
          PaymentInfo(
            it.pagopa.ecommerce.payment.requests.domain.RptId(p.fiscalCode + p.noticeNumber),
            p.description,
            p.amount,
            p.companyName)
        }
      },
      extractReturnUrls = { dto ->
        ReturnUrls(
          returnSuccessUrl = dto.returnUrls.returnOkUrl.toString(),
          returnErrorUrl = dto.returnUrls.returnErrorUrl.toString(),
          returnCancelUrl = dto.returnUrls.returnCancelUrl.toString())
      },
      extractEmail = { it.emailNotice },
      extractIdCart = { it.idCart },
      extractSavedCartId = { it.id },
      getClientIdValue = { it.value },
      checkPosition = { payments ->
        val dto =
          CheckPositionDto()
            .positionslist(
              payments.map {
                ListelementRequestDto()
                  .fiscalCode(it.rptId.fiscalCode)
                  .noticeNumber(it.rptId.noticeId)
              })
        nodoPerPmClient.checkPosition(dto).awaitSingle()
      })
  }

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
              returnErrorUrl = URI(it.returnErrorUrl))
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
