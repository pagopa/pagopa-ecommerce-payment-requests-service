package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import java.time.Duration
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
class PaymentRequestsRedisTemplateWrapperTest {

  private val redisTemplate: RedisTemplate<String, PaymentRequestInfo> = mock()

  private val valueOperations: ValueOperations<String, PaymentRequestInfo> = mock()

  @Captor private lateinit var keyCaptor: ArgumentCaptor<String>

  private val paymentRequestsRedisTemplateWrapper: PaymentRequestsRedisTemplateWrapper =
    PaymentRequestsRedisTemplateWrapper(redisTemplate, Duration.ofMinutes(10))

  @Test
  fun `Should save entity successfully`() {
    // pre-requisites
    val rptIdAsString = "77777777777302016723749670035"
    val rptIdAsObject = RptId(rptIdAsString)
    val paTaxCode = "77777777777"
    val paName = "Pa Name"
    val description = "Payment request description"
    val amount = Integer.valueOf(1000)
    val paymentRequestInfo =
      PaymentRequestInfo(
        rptIdAsObject, paTaxCode, paName, description, amount, null, null, null, null)
    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    doNothing()
      .`when`(valueOperations)
      .set(capture(keyCaptor), eq(paymentRequestInfo), eq(Duration.ofMinutes(10)))

    // test
    paymentRequestsRedisTemplateWrapper.save(paymentRequestInfo)
    assertEquals("keys:$rptIdAsString", keyCaptor.value)
  }

  @Test
  fun `Should get entity successfully`() {
    // pre-requisites
    val rptIdAsString = "77777777777302016723749670035"
    val rptIdAsObject = RptId(rptIdAsString)
    val paTaxCode = "77777777777"
    val paName = "Pa Name"
    val description = "Payment request description"
    val amount = Integer.valueOf(1000)
    val paymentRequestInfo =
      PaymentRequestInfo(
        rptIdAsObject, paTaxCode, paName, description, amount, null, null, null, null)
    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    given(valueOperations.get("keys:$rptIdAsString")).willReturn(paymentRequestInfo)

    // test
    val getPaymentRequestInfo = paymentRequestsRedisTemplateWrapper.findById(rptIdAsString)
    assertEquals(paymentRequestInfo, getPaymentRequestInfo)
  }
}
