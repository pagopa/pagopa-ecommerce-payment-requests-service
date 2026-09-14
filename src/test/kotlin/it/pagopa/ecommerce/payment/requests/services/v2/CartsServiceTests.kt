package it.pagopa.ecommerce.payment.requests.services.v2

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.payment.requests.server.v2.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.tests.utils.v2.CartRequests
import it.pagopa.ecommerce.payment.requests.utils.TokenizerEmailUtils
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
class CartsServiceTests {
  companion object {
    const val TEST_CHECKOUT_URL: String = "https://test-checkout.url"
  }

  private val cartRedisTemplateWrapper: CartsRedisTemplateWrapper = mock()
  private val nodoPerPmClient: NodoPerPmClient = mock()
  private val tokenizerMailUtils: TokenizerEmailUtils = mock()
  private val cartsMaxAllowedPaymentNotices = 5
  private val cartService: CartService =
    CartService(
      "${TEST_CHECKOUT_URL}/c/{0}?clientId={1}",
      cartRedisTemplateWrapper,
      nodoPerPmClient,
      tokenizerMailUtils,
      cartsMaxAllowedPaymentNotices)

  @Test
  fun `v2 post cart succeeded with one payment notice`() = runTest {
    val cartId = UUID.randomUUID()
    val tokenizedEmail = UUID.randomUUID()
    val clientId = ClientIdDto.WISP_REDIRECT

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)))
      val request = CartRequests.withOnePaymentNotice()
      val locationUrl = "${TEST_CHECKOUT_URL}/c/${cartId}?clientId=${clientId.value}"
      given(tokenizerMailUtils.toConfidential(Email(request.emailNotice)))
        .willReturn(Mono.just(Confidential<Email>(tokenizedEmail.toString())))
      given(cartRedisTemplateWrapper.save(any())).willReturn(Mono.just(true))
      assertEquals(locationUrl, cartService.processCart(clientId, request))
      verify(cartRedisTemplateWrapper, times(1)).save(any())
    }
  }

  @Test
  fun `post cart succeeded with one payment notice without mail`() = runTest {
    val cartId = UUID.randomUUID()
    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)))
      given(cartRedisTemplateWrapper.save(any())).willReturn(Mono.just(true))
      val request = CartRequests.withOnePaymentNotice(null)
      val clientId = ClientIdDto.WISP_REDIRECT
      val locationUrl = "${TEST_CHECKOUT_URL}/c/${cartId}?clientId=${clientId.value}"
      assertEquals(locationUrl, cartService.processCart(clientId, request))
      verify(cartRedisTemplateWrapper, times(1)).save(any())
      verify(tokenizerMailUtils, times(0)).toConfidential(any<Email>())
    }
  }

  @Test
  fun `v2 post cart failed with checkValidity KO`() = runTest {
    val cartId = UUID.randomUUID()
    val tokenizedEmail = UUID.randomUUID()

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.KO)))
      val request = CartRequests.withOnePaymentNotice()
      val clientId = ClientIdDto.WISP_REDIRECT
      given(tokenizerMailUtils.toConfidential(request.emailNotice))
        .willReturn(Mono.just(Confidential<Email>(tokenizedEmail.toString())))

      assertThrows<RestApiException> { cartService.processCart(clientId, request) }
    }
  }

  @Test
  fun `v2 post cart failed with duplicate payments`() = runTest {
    val cartId = UUID.randomUUID()
    val clientId = ClientIdDto.WISP_REDIRECT

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)))
      val request = CartRequests.withMultiplePaymentNotices(2)
      val exc = assertThrows<RestApiException> { cartService.processCart(clientId, request) }
      assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exc.httpStatus)
      assertEquals("Duplicate payment notice values found.", exc.description)
    }
  }

  @Test
  fun `v2 post cart ko with multiple payment notices`() = runTest {
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices + 1)
    val clientId = ClientIdDto.WISP_REDIRECT

    assertThrows<RestApiException> { cartService.processCart(clientId, request) }
  }
}
