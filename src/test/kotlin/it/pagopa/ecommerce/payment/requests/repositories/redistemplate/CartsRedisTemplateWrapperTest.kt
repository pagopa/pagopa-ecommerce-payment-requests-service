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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

@ExtendWith(MockitoExtension::class)
class CartsRedisTemplateWrapperTest {

  private val redisTemplate: RedisTemplate<String, CartInfo> = mock()

  private val valueOperations: ValueOperations<String, CartInfo> = mock()

  @Captor private lateinit var keyCaptor: ArgumentCaptor<String>

  private val cartsRedisTemplateWrapper: CartsRedisTemplateWrapper =
    CartsRedisTemplateWrapper(redisTemplate, Duration.ofMinutes(10))

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
    doNothing()
      .`when`(valueOperations)
      .set(capture(keyCaptor), eq(cartInfo), eq(Duration.ofMinutes(10)))

    // test
    cartsRedisTemplateWrapper.save(cartInfo)
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
    given(valueOperations.get("carts:$cartId")).willReturn(cartInfo)

    // test
    val getPaymentRequestInfo = cartsRedisTemplateWrapper.findById(cartId.toString())
    assertEquals(cartInfo, getPaymentRequestInfo)
  }
}
