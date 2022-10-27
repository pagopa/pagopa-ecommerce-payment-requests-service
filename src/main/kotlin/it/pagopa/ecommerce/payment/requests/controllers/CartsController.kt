package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.CartsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class CartsController(
) : CartsApi {

    @Autowired
    lateinit var cartService: CartService

    override suspend fun postCarts(cartRequestDto: CartRequestDto): ResponseEntity<Unit> {
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(
                URI.create(cartService.processCart(cartRequestDto))
            ).build()
    }


    override suspend fun getCarts(idCart: String): ResponseEntity<CartRequestDto> {
        return ResponseEntity.ok(
            cartService.getCart(idCart)
        )
    }
}