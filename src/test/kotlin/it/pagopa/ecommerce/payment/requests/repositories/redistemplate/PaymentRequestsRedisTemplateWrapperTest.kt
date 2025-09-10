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
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class PaymentRequestsRedisTemplateWrapperTest {

  private val redisTemplate: ReactiveRedisTemplate<String, PaymentRequestInfo> = mock()

  private val valueOperations: ReactiveValueOperations<String, PaymentRequestInfo> = mock()

  @Captor private lateinit var keyCaptor: ArgumentCaptor<String>

  private val duration = Duration.ofMinutes(10)

  private val paymentRequestsRedisTemplateWrapper: PaymentRequestsRedisTemplateWrapper =
    PaymentRequestsRedisTemplateWrapper(redisTemplate, duration)

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
        rptIdAsObject,
        paTaxCode,
        paName,
        description,
        amount,
        null,
        null,
        null,
        null,
        null,
        null,
        null)
    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    given(valueOperations.set(capture(keyCaptor), eq(paymentRequestInfo), eq(duration)))
      .willReturn(Mono.just(true))

    // test
    StepVerifier.create(paymentRequestsRedisTemplateWrapper.save(paymentRequestInfo))
      .expectNext(true)
      .verifyComplete()

    verify(redisTemplate, times(1)).opsForValue()
    verify(valueOperations, times(1)).set("keys:$rptIdAsString", paymentRequestInfo, duration)
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
        rptIdAsObject,
        paTaxCode,
        paName,
        description,
        amount,
        null,
        null,
        null,
        null,
        null,
        null,
        null)
    given(redisTemplate.opsForValue()).willReturn(valueOperations)
    given(valueOperations.get("keys:$rptIdAsString")).willReturn(Mono.just(paymentRequestInfo))

    // test
    StepVerifier.create(paymentRequestsRedisTemplateWrapper.findById(rptIdAsString))
      .expectNext(paymentRequestInfo)
      .verifyComplete()

    verify(redisTemplate, times(1)).opsForValue()
  }
}
