package it.pagopa.ecommerce.payment.requests.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.payment.requests.server.model.*
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.CheckPositionErrorException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
import it.pagopa.ecommerce.payment.requests.validation.BeanValidationConfiguration
import java.net.URI
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(CartsController::class)
@Import(BeanValidationConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class CartsControllerTests {

  @Autowired lateinit var webClient: WebTestClient

  @MockBean lateinit var cartService: CartService

  @Mock private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

  @Mock private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>

  @Mock private lateinit var responseSpec: WebClient.ResponseSpec

  @InjectMocks val cartsController: CartsController = CartsController()

  private val cartsMaxAllowedPaymentNotices = 5

  private val objectMapper = ObjectMapper()

  @Test
  fun `post cart succeeded with one payment notice`() = runTest {
    val request = CartRequests.withOnePaymentNotice()
    val clientId = ClientIdDto.WISP_REDIRECT
    val locationUrl = "http://checkout-url.it/77777777777302000100440009424?clientId=WISP_REDIRECT"
    given(cartService.processCart(clientId, request)).willReturn(locationUrl)
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .is3xxRedirection
      .expectHeader()
      .location(locationUrl)
  }

  @Test
  fun `post cart KO with multiple payment notices`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices)
    val clientId = ClientIdDto.WISP_REDIRECT
    given(cartService.processCart(clientId, request))
      .willThrow(
        RestApiException(
          httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
          title = "Multiple payment notices not processable",
          description = "Too many payment notices, expected max one"))
    val errorResponse =
      ProblemJsonDto(
        status = 422,
        title = "Multiple payment notices not processable",
        detail = "Too many payment notices, expected max one")

    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(422)
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `post cart KO with internal server error while invoke checkPosition`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices)
    val clientId = ClientIdDto.WISP_REDIRECT
    given(cartService.processCart(clientId, request))
      .willThrow(CheckPositionErrorException(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR))
    val errorResponse = ProblemJsonDto(status = 502, title = "Bad gateway")
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(502)
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `post cart KO with 404 while invoke checkPosition`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices)
    val clientId = ClientIdDto.WISP_REDIRECT
    given(cartService.processCart(clientId, request))
      .willThrow(CheckPositionErrorException(httpStatus = HttpStatus.NOT_FOUND))
    val errorResponse = ProblemJsonDto(status = 500, title = "Internal server error")
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(500)
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `post cart KO with 400 while invoke checkPosition`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices)
    val clientId = ClientIdDto.WISP_REDIRECT
    given(cartService.processCart(clientId, request))
      .willThrow(CheckPositionErrorException(httpStatus = HttpStatus.UNPROCESSABLE_ENTITY))
    val errorResponse = ProblemJsonDto(status = 422, title = "Invalid payment info")
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(422)
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `invalid request ko`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.invalidRequest()
    val clientId = ClientIdDto.WISP_REDIRECT
    val errorResponse =
      ProblemJsonDto(
        status = 400, title = "Request validation error", detail = "The input request is invalid")
    given(cartService.processCart(clientId, request)).willReturn("")
    webClient
      .post()
      .uri("/carts")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `controller throw generic exception`() = runTest {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val request = CartRequests.withOnePaymentNotice()
    val clientId = ClientIdDto.WISP_REDIRECT
    val errorResponse =
      ProblemJsonDto(
        title = "Error processing the request",
        detail = "An internal error occurred processing the request",
        status = 500)
    given(cartService.processCart(clientId, request))
      .willThrow(RuntimeException("Test unmanaged exception"))
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .json(objectMapper.writeValueAsString(errorResponse))
  }

  @Test
  fun `get cart by id`() = runTest {
    val cartId = UUID.randomUUID()
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val response =
      CartRequestDto(
        paymentNotices =
          listOf(
            PaymentNoticeDto(
              noticeNumber = "",
              fiscalCode = "",
              amount = 10000,
              companyName = "companyName",
              description = "description")),
        returnUrls =
          CartRequestReturnUrlsDto(
            returnErrorUrl = URI.create("https://returnErrorUrl"),
            returnOkUrl = URI.create("https://returnOkUrl"),
            returnCancelUrl = URI.create("https://returnCancelUrl")),
        emailNotice = "test@test.it")
    given(cartService.getCart(cartId)).willReturn(response)
    val parameters = mapOf("idCart" to cartId)
    webClient
      .get()
      .uri("/carts/{idCart}", parameters)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(objectMapper.writeValueAsString(response))
  }

  @Test
  fun `get cart by id without mail`() = runTest {
    val cartId = UUID.randomUUID()
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    val response =
      CartRequestDto(
        paymentNotices =
          listOf(
            PaymentNoticeDto(
              noticeNumber = "",
              fiscalCode = "",
              amount = 10000,
              companyName = "companyName",
              description = "description")),
        returnUrls =
          CartRequestReturnUrlsDto(
            returnErrorUrl = URI.create("https://returnErrorUrl"),
            returnOkUrl = URI.create("https://returnOkUrl"),
            returnCancelUrl = URI.create("https://returnCancelUrl")))
    given(cartService.getCart(cartId)).willReturn(response)
    val parameters = mapOf("idCart" to cartId)
    webClient
      .get()
      .uri("/carts/{idCart}", parameters)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(objectMapper.writeValueAsString(response))
  }

  @Test
  fun `get cart by id with non-existing cart returns 404`() = runTest {
    val cartId = UUID.randomUUID()
    val exception = CartNotFoundException(cartId.toString())
    val expected =
      ProblemJsonDto(
        title = "Cart not found",
        detail = exception.message ?: "",
        status = HttpStatus.NOT_FOUND.value())

    given(cartService.getCart(cartId)).willThrow(exception)

    val parameters = mapOf("cartId" to cartId)
    webClient
      .get()
      .uri("/carts/{cartId}", parameters)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody<ProblemJsonDto>()
      .isEqualTo(expected)
  }

  @Test
  fun `warm up controller`() {
    val webClient = mock(WebClient::class.java)
    given(webClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
      .willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(CartRequestDto::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatusCode>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.toBodilessEntity()).willReturn(Mono.empty())
    CartsController(webClient).warmupPostCarts()
    verify(webClient, times(1)).post()
  }

  @ParameterizedTest
  @ValueSource(strings = ["foo@foo.it", "FOO@FOO.IT", "FoO@fOo.It"])
  @NullSource
  fun `post cart succeeded with mail multiple case`(email: String?) = runTest {
    val request = CartRequests.withOnePaymentNotice(email)
    val clientId = ClientIdDto.WISP_REDIRECT
    val locationUrl = "http://checkout-url.it/77777777777302000100440009424?clientId=WISP_REDIRECT"
    given(cartService.processCart(clientId, request)).willReturn(locationUrl)
    webClient
      .post()
      .uri("/carts")
      .header("x-client-id", ClientIdDto.WISP_REDIRECT.value)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .is3xxRedirection
      .expectHeader()
      .location(locationUrl)
  }

  @Test
  fun `post cart KO with invalid email`() = runTest {
    val request = CartRequests.withOnePaymentNotice("email")
    val clientId = ClientIdDto.WISP_REDIRECT
    val locationUrl = "http://checkout-url.it/77777777777302000100440009424?clientId=WISP_REDIRECT"
    val errorResponse =
      ProblemJsonDto(
        title = "Request validation error", detail = "The input request is invalid", status = 400)
    given(cartService.processCart(clientId, request)).willReturn(locationUrl)
    webClient
      .post()
      .uri("/carts")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody<ProblemJsonDto>()
      .isEqualTo(errorResponse)
  }
}
