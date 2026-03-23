package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import java.text.MessageFormat
import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus

object CartServiceHelper {

  suspend fun <
    CartRequestDtoType, ReturnUrlsType, CartInfoType, IdType, ClientIdDtoType> processCart(
    xClientId: ClientIdDtoType,
    cartRequestDto: CartRequestDtoType,
    maxAllowedPaymentNotices: Int,
    checkoutUrl: String,
    tokenizerMailUtils: TokenizerEmailUtils,
    saveCart: suspend (CartInfoType) -> CartInfoType,
    mapToCartInfo:
      (
        cartId: UUID,
        payments: List<PaymentInfo>,
        idCart: IdType,
        returnUrls: ReturnUrlsType,
        email: String?) -> CartInfoType,
    extractPaymentNotices: (CartRequestDtoType) -> List<PaymentInfo>,
    extractReturnUrls: (CartRequestDtoType) -> ReturnUrlsType,
    extractEmail: (CartRequestDtoType) -> String?,
    extractIdCart: (CartRequestDtoType) -> IdType,
    extractSavedCartId: (CartInfoType) -> UUID,
    getClientIdValue: (ClientIdDtoType) -> String,
    checkPosition: suspend (List<PaymentInfo>) -> CheckPositionResponseDto
  ): String {
    val paymentsNotices = extractPaymentNotices(cartRequestDto)
    val receivedNotices = paymentsNotices.size

    if (receivedNotices > maxAllowedPaymentNotices) {
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Multiple payment notices not processable",
        description = "Too many payment notices, expected max $maxAllowedPaymentNotices")
    }

    if (receivedNotices != paymentsNotices.map { it.rptId }.toSet().size) {
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Invalid payment info",
        description = "Duplicate payment notice values found.")
    }

    val checkResponse = checkPosition(paymentsNotices)
    if (checkResponse.outcome != CheckPositionResponseDto.OutcomeEnum.OK) {
      throw RestApiException(
        httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
        title = "Invalid payment info",
        description = "Invalid payment notice data")
    }

    val id = UUID.randomUUID()
    val returnUrls = extractReturnUrls(cartRequestDto)
    val email = extractEmail(cartRequestDto)
    val idCart = extractIdCart(cartRequestDto)

    val cartInfo =
      if (email != null) {
        val tokenizedEmail = tokenizerMailUtils.toConfidential(Email(email)).awaitSingle()
        mapToCartInfo(id, paymentsNotices, idCart, returnUrls, tokenizedEmail.opaqueData)
      } else {
        mapToCartInfo(id, paymentsNotices, idCart, returnUrls, null)
      }

    val savedCart = saveCart(cartInfo)
    val savedCartId = extractSavedCartId(savedCart)

    return MessageFormat.format(checkoutUrl, savedCartId, getClientIdValue(xClientId))
  }
}
