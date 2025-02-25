package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.ClientIdDto
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
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
  fun `post cart succeeded with one payment notice`() = runTest {
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
      val request = CartRequests.withOnePaymentNotice(null)
      val clientId = ClientIdDto.WISP_REDIRECT
      val locationUrl = "${TEST_CHECKOUT_URL}/c/${cartId}?clientId=${clientId.value}"
      assertEquals(locationUrl, cartService.processCart(clientId, request))
      verify(cartRedisTemplateWrapper, times(1)).save(any())
      verify(tokenizerMailUtils, times(0)).toConfidential(any<Email>())
    }
  }

  @Test
  fun `post cart failed with checkValidity KO`() = runTest {
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
  fun `post cart failed with duplicate payments`() = runTest {
    val cartId = UUID.randomUUID()
    val clientId = ClientIdDto.WISP_REDIRECT

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)))
      val request = CartRequests.withMultiplePaymentNotices(2)
      val exc = assertThrows<RestApiException> { cartService.processCart(clientId, request) }
      assertEquals(HttpStatus.BAD_REQUEST, exc.httpStatus)
    }
  }

  @Test
  fun `post cart ko with multiple payment notices`() = runTest {
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices + 1)
    val clientId = ClientIdDto.WISP_REDIRECT

    assertThrows<RestApiException> { cartService.processCart(clientId, request) }
  }

  @Test
  fun `get cart by id`() = runTest {
    val cartId = UUID.randomUUID()
    val clearEmail = "test@test.it"

    val cart = CartRequests.withOnePaymentNotice()
    val expectedCart =
      CartRequestDto(
        paymentNotices = cart.paymentNotices,
        returnUrls = cart.returnUrls,
        emailNotice = clearEmail,
        idCart = cart.idCart)

    given(tokenizerMailUtils.toEmail(Confidential(cart.emailNotice)))
      .willReturn(Mono.just(Email(clearEmail)))

    given(cartRedisTemplateWrapper.findById(cartId.toString()))
      .willReturn(
        cart.let { req ->
          CartInfo(
            cartId,
            req.paymentNotices.map {
              PaymentInfo(
                RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
            },
            req.idCart,
            req.returnUrls.let {
              ReturnUrls(
                returnSuccessUrl = it.returnOkUrl.toString(),
                returnErrorUrl = it.returnErrorUrl.toString(),
                returnCancelUrl = it.returnCancelUrl.toString())
            },
            req.emailNotice)
        })

    assertEquals(expectedCart, cartService.getCart(cartId))
  }

  @Test
  fun `get cart by id without mail`() = runTest {
    val cartId = UUID.randomUUID()

    val cart = CartRequests.withOnePaymentNotice()
    val expectedCart =
      CartRequestDto(
        paymentNotices = cart.paymentNotices, returnUrls = cart.returnUrls, idCart = cart.idCart)

    given(cartRedisTemplateWrapper.findById(cartId.toString()))
      .willReturn(
        cart.let { req ->
          CartInfo(
            cartId,
            req.paymentNotices.map {
              PaymentInfo(
                RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
            },
            req.idCart,
            req.returnUrls.let {
              ReturnUrls(
                returnSuccessUrl = it.returnOkUrl.toString(),
                returnErrorUrl = it.returnErrorUrl.toString(),
                returnCancelUrl = it.returnCancelUrl.toString())
            },
            null)
        })

    assertEquals(expectedCart, cartService.getCart(cartId))
    verify(tokenizerMailUtils, times(0)).toEmail(any())
  }

  @Test
  fun `non-existing id throws CartNotFoundException`() = runTest {
    val cartId = UUID.randomUUID()
    Mockito.mockStatic(UUID::class.java).use {
      given(cartRedisTemplateWrapper.findById(cartId.toString())).willReturn(null)
      assertThrows<CartNotFoundException> { cartService.getCart(cartId) }
    }
  }
}
