package it.pagopa.ecommerce.payment.requests.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.ProblemJsonDto
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
import it.pagopa.ecommerce.payment.requests.validation.BeanValidationConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.net.URI
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(CartsController::class)
@Import(BeanValidationConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class CartsControllerTests {

    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    lateinit var cartService: CartService

    @InjectMocks
    val cartsController: CartsController = CartsController()

    @Test
    fun `post cart succeeded with one payment notice`() = runTest {
        val request = CartRequests.withOnePaymentNotice()
        val locationUrl = "http://checkout-url.it/77777777777302000100440009424"
        given(cartService.processCart(request)).willReturn(locationUrl)
        webClient.post()
            .uri("/carts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader().location(locationUrl)
    }

    @Test
    fun `post cart KO with multiple payment notices`() = runTest {
        val objectMapper = ObjectMapper()
        val request = CartRequests.withMultiplePaymentNotice()
        given(cartService.processCart(request)).willThrow(
            RestApiException(
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                title = "Multiple payment notices not processable",
                description = "Too many payment notices, expected max one"
            )
        )
        val errorResponse = ProblemJsonDto(
            status = 422,
            title = "Multiple payment notices not processable",
            detail = "Too many payment notices, expected max one"
        )

        webClient.post()
            .uri("/carts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody().json(objectMapper.writeValueAsString(errorResponse))
    }

    @Test
    fun `invalid request ko`() = runTest {
        val objectMapper = ObjectMapper()
        val request = CartRequests.invalidRequest()
        val errorResponse = ProblemJsonDto(
            status = 400,
            title = "Request validation error",
            detail = "The input request is invalid"
        )
        given(cartService.processCart(request)).willReturn("")
        webClient.post()
            .uri("/carts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().json(objectMapper.writeValueAsString(errorResponse))
    }

    @Test
    fun `controller throw generic exception`() = runTest {
        val objectMapper = ObjectMapper()
        val request = CartRequests.withOnePaymentNotice()
        val errorResponse = ProblemJsonDto(
            title = "Error processing the request",
            detail = "An internal error occurred processing the request",
            status = 500
        )
        given(cartService.processCart(request)).willThrow(RuntimeException("Test unmanaged exception"))
        webClient.post()
            .uri("/carts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody().json(objectMapper.writeValueAsString(errorResponse))
    }

    @Test
    fun `get cart by id`() {
        val cartId = "1234"
        val objectMapper = ObjectMapper()
        val response = CartRequestDto(
            paymentNotices = listOf(
                PaymentNoticeDto(
                    noticeNumber = "",
                    fiscalCode = "",
                    amount = 10000
                )
            ),
            returnUrls = CartRequestReturnUrlsDto(
                returnErrorUrl = URI.create("https://returnErrorUrl"),
                returnOkUrl = URI.create("https://returnOkUrl"),
                returnCancelUrl = URI.create("https://returnCancelUrl")
            ),
            emailNotice = "test@test.it"
        )
        given(cartService.getCart(cartId)).willReturn(response)
        val parameters = mapOf("idCart" to cartId)
        webClient.get()
            .uri("/carts/{idCart}", parameters)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(objectMapper.writeValueAsString(response))
    }

    @Test
    fun `get cart by id with non-existing cart returns 404`() {
        val cartId = UUID.randomUUID().toString()
        val exception = CartNotFoundException(cartId)
        val expected = ProblemJsonDto(
            title = "Cart not found",
            detail = exception.message ?: "",
            status = HttpStatus.NOT_FOUND.value()
        )

        given(cartService.getCart(cartId)).willThrow(exception)

        val parameters = mapOf("cartId" to cartId)
        webClient.get()
            .uri("/carts/{cartId}", parameters)
            .exchange()
            .expectStatus().isNotFound
            .expectBody<ProblemJsonDto>()
            .isEqualTo(expected)
    }
}