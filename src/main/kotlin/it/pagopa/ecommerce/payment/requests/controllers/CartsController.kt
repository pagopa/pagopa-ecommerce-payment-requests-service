package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.CartsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import it.pagopa.ecommerce.payment.requests.warmup.exceptions.WarmUpException
import it.pagopa.ecommerce.payment.requests.warmup.utils.WarmupRequests
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
class CartsController(
  private val webClient: WebClient = WebClient.create(),
  @Value("\${security.apiKey.primary}") private val primaryApiKey: String,
) : CartsApi {
  @Autowired private lateinit var cartService: CartService

  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/carts", "/carts/"],
    produces = ["application/json"],
    consumes = ["application/json"])
  override suspend fun postCarts(
    xClientId: ClientIdDto,
    cartRequestDto: CartRequestDto
  ): ResponseEntity<Unit> {
    return ResponseEntity.status(HttpStatus.FOUND)
      .location(URI.create(cartService.processCart(xClientId, cartRequestDto)))
      .build()
  }

  override suspend fun getCarts(idCart: java.util.UUID): ResponseEntity<CartRequestDto> {
    return ResponseEntity.ok(cartService.getCart(idCart))
  }

  /** Controller warm up function, used to send a POST carts request */
  @WarmupFunction
  fun warmupPostCarts() {
    webClient
      .post()
      .uri("http://localhost:8080/carts")
      .header("x-api-key", primaryApiKey)
      .header("x-client-id", "CHECKOUT")
      .body(Mono.just(WarmupRequests.postCartsReq()), CartRequestDto::class.java)
      .retrieve()
      .onStatus(HttpStatusCode::isError) {
        Mono.error(WarmUpException("CartsController", "warmupPostCarts"))
      }
      .toBodilessEntity()
      .block(Duration.of(10, ChronoUnit.SECONDS))
  }
}
