package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.CartsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import it.pagopa.ecommerce.payment.requests.warmup.utils.WarmupRequests
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URI

@RestController
class CartsController(
    private val restTemplate: RestTemplate = RestTemplate()
) : CartsApi {
    @Autowired
    private lateinit var cartService: CartService

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

    /**
     * Controller warm up function, used to send a POST carts request
     */
    @WarmupFunction
    fun warmupPostCarts() {
        restTemplate.postForLocation("http://localhost:8080/carts", WarmupRequests.postCartsReq())
    }
}