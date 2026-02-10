package it.pagopa.ecommerce.payment.requests.controllers.v2

import it.pagopa.ecommerce.generated.payment.requests.server.v2.api.CartsApiV2Api
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.CartRequestV2Dto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@RestController
class CartsControllerV2 (
    private val webClient: WebClient = WebClient.create(),
    @Value("\${security.apiKey.primary}") private val primaryApiKey: String
): CartsApiV2Api{
    @Autowired private lateinit var cartService: CartService
    override suspend fun postCartsV2(
        xClientId: ClientIdDto,
        cartRequestV2Dto: CartRequestV2Dto
    ): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(cartService.processCartV2(xClientId, cartRequestV2Dto)))
            .build()
    }
}