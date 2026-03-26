package it.pagopa.ecommerce.payment.requests.services.v2

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.ListelementRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.v2.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.repositories.v2.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.v2.ReturnUrls
import it.pagopa.ecommerce.payment.requests.services.CartServiceHelper
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("CartServiceV2")
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
          returnCancelUrl = dto.returnUrls.returnCancelUrl.toString(),
          returnWaitingUrl = dto.returnUrls.returnWaitingUrl.toString())
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
}
