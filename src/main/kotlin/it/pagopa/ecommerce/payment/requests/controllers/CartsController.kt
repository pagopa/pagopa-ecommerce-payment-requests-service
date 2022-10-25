package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.CartsApi
import org.springframework.web.bind.annotation.RestController
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnurlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI
import java.util.*

@RestController
class CartsController(
): CartsApi{

    override suspend fun postCarts(cartRequestDto: CartRequestDto): ResponseEntity<Unit> {
        return  ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("https://dev.checkout.pagopa.it"))
            .body(Unit)
    }

    override suspend fun getCarts(idCart: String): ResponseEntity<CartRequestDto> {
        return ResponseEntity.ok(
            CartRequestDto(
                paymentNotices = listOf(PaymentNoticeDto()),
                returnurls = CartRequestReturnurlsDto(
                    retunErrorOk = URI.create("https://returnErrorUrl"),
                    returnOkUrl = URI.create("https://returnOkUrl"),
                    returnCancelUrl = URI.create("https://returnCancelUrl")
                ),
                emailNotice = "test@test.it"
            )
        )
    }
}
