package it.pagopa.ecommerce.payment.requests.controllers.v2

import it.pagopa.ecommerce.generated.payment.requests.server.v2.api.CartsApi
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.services.v2.CartService
import java.net.URI
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient

@RestController
class CartsController(
  private val webClient: WebClient = WebClient.create(),
  @Value("\${security.apiKey.primary}") private val primaryApiKey: String
) : CartsApi {
  @Autowired private lateinit var cartService: CartService
  override suspend fun postCarts(
    xClientId: ClientIdDto,
    cartRequestDto: CartRequestDto
  ): ResponseEntity<Unit> {
    return ResponseEntity.status(HttpStatus.FOUND)
      .location(URI.create(cartService.processCart(xClientId, cartRequestDto)))
      .build()
  }
}
