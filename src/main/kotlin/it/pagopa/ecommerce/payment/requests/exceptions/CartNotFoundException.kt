package it.pagopa.ecommerce.payment.requests.exceptions

import it.pagopa.ecommerce.payment.requests.errorhandling.ApiError
import org.springframework.http.HttpStatus

class CartNotFoundException(cartId: String): ApiError("Cart with id $cartId not found") {
    override fun toRestException(): RestApiException {
        return RestApiException(
            httpStatus = HttpStatus.NOT_FOUND,
            title = "Cart not found",
            description = this.message ?: ""
        )
    }
}
