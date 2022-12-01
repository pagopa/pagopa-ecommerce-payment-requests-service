package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.CartsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import it.pagopa.ecommerce.payment.requests.warmup.exceptions.WarmUpException
import it.pagopa.ecommerce.payment.requests.warmup.utils.WarmupRequests
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit

@RestController
class CartsController(
    private val webClient: WebClient = WebClient.create()
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


    override suspend fun getCarts(idCart: java.util.UUID): ResponseEntity<CartRequestDto> {
        return ResponseEntity.ok(
            cartService.getCart(idCart)
        )
    }

    /**
     * Controller warm up function, used to send a POST carts request
     */
    @WarmupFunction
    fun warmupPostCarts() {
        webClient.post().uri("http://localhost:8080/carts")
            .body(Mono.just(WarmupRequests.postCartsReq()), CartRequestDto::class.java)
            .retrieve()
            .onStatus(HttpStatus::isError) { Mono.error(WarmUpException("CartsController", "warmupPostCarts")) }
            .toBodilessEntity().block(Duration.of(10, ChronoUnit.SECONDS))
    }
}