package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
import java.time.Duration
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class CartsRedisTemplateWrapperTest {

  private val redisTemplate: ReactiveRedisTemplate<String, CartInfo> = mock()

  private val valueOperations: ReactiveValueOperations<String, CartInfo> = mock()

  @Captor private lateinit var keyCaptor: ArgumentCaptor<String>

  private val duration = Duration.ofMinutes(10)

  private val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper =
    CartsRedisTemplateWrapper(redisTemplate, duration)

  @Test
  fun `Should save entity successfully`() {
    // pre-requisites
    val cartId = UUID.randomUUID()
    val cartRequest = CartRequests.withMultiplePaymentNotices(5)
    val cartInfo =
      cartRequest.let { req ->
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
      }
    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    given(valueOperations.set(capture(keyCaptor), eq(cartInfo), eq(duration)))
      .willReturn(Mono.just(true))

    // test
    StepVerifier.create(cartsRedisTemplateWrapper.save(cartInfo)).expectNext(true).verifyComplete()

    verify(redisTemplate, times(1)).opsForValue()
    verify(valueOperations, times(1)).set("carts:$cartId", cartInfo, duration)
    assertEquals("carts:$cartId", keyCaptor.value)
  }

  @Test
  fun `Should get entity successfully`() {
    // pre-requisites
    val cartId = UUID.randomUUID()
    val cartRequest = CartRequests.withMultiplePaymentNotices(5)
    val cartInfo =
      cartRequest.let { req ->
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
      }

    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    given(valueOperations.get("carts:$cartId")).willReturn(Mono.just(cartInfo))

    // test
    StepVerifier.create(cartsRedisTemplateWrapper.findById(cartId.toString()))
      .expectNext(cartInfo)
      .verifyComplete()

    verify(redisTemplate, times(1)).opsForValue()
  }
}
