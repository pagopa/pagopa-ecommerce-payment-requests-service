package it.pagopa.ecommerce.payment.requests.services.v2

import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.services.BaseCartService
import it.pagopa.ecommerce.payment.requests.services.CartRequest
import it.pagopa.ecommerce.payment.requests.services.PaymentNoticeData
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import java.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("CartServiceV2")
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
      returnWaitingUrl = request.returnWaitingUrl)

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
          returnWaitingUrl = dto.returnUrls.returnWaitingUrl.toString(),
          emailNotice = dto.emailNotice,
          idCart = dto.idCart))
}
