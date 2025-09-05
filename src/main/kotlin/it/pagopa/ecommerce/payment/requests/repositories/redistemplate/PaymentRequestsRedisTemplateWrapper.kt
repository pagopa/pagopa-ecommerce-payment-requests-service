package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

class PaymentRequestsRedisTemplateWrapper(
  reactiveRedisTemplate: ReactiveRedisTemplate<String, PaymentRequestInfo>,
  ttl: Duration
) :
  ReactiveRedisTemplateWrapper<PaymentRequestInfo>(
    reactiveRedisTemplate = reactiveRedisTemplate, "keys", ttl) {
  override fun getKeyFromEntity(value: PaymentRequestInfo): String =
    "${value.id.fiscalCode}${value.id.noticeId}"
}
