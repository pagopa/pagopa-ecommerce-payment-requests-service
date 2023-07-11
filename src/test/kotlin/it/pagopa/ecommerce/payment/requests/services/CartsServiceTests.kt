package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
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
      "${TEST_CHECKOUT_URL}/c/{0}",
      cartRedisTemplateWrapper,
      nodoPerPmClient,
      tokenizerMailUtils,
      cartsMaxAllowedPaymentNotices)

  @Test
  fun `post cart succeeded with one payment notice`() = runTest {
    val cartId = UUID.randomUUID()
    val tokenizedEmail = UUID.randomUUID()

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)
      given(nodoPerPmClient.checkPosition(any()))
        .willReturn(
          Mono.just(CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)))
      val request = CartRequests.withOnePaymentNotice()
      val locationUrl = "${TEST_CHECKOUT_URL}/c/${cartId}"
      given(tokenizerMailUtils.toConfidential(request.emailNotice))
        .willReturn(Mono.just(Confidential<Email>(tokenizedEmail.toString())))
      assertEquals(locationUrl, cartService.processCart(request))
      verify(cartRedisTemplateWrapper, times(1)).save(any())
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
      given(tokenizerMailUtils.toConfidential(request.emailNotice))
        .willReturn(Mono.just(Confidential<Email>(tokenizedEmail.toString())))

      assertThrows<RestApiException> { cartService.processCart(request) }
    }
  }

  @Test
  fun `post cart ko with multiple payment notices`() = runTest {
    val request = CartRequests.withMultiplePaymentNotices(cartsMaxAllowedPaymentNotices + 1)
    assertThrows<RestApiException> { cartService.processCart(request) }
  }

  @Test
  suspend fun `get cart by id`() = runTest {
    val cartId = UUID.randomUUID()
    val tokenizedEmail = UUID.randomUUID().toString()

    Mockito.mockStatic(UUID::class.java).use { uuidMock ->
      uuidMock.`when`<UUID> { UUID.fromString(cartId.toString()) }.thenReturn(cartId)

      val request = CartRequests.withOnePaymentNotice()

      given(tokenizerMailUtils.toEmail(Confidential(tokenizedEmail)))
        .willReturn(Mono.just(Email(request.emailNotice)))

      given(cartRedisTemplateWrapper.findById(cartId.toString()))
        .willReturn(
          request.let { req ->
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

      assertEquals(request, cartService.getCart(cartId))
    }
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
