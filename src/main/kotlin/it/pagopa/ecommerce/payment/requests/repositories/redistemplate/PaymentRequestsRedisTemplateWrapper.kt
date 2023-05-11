package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

class PaymentRequestsRedisTemplateWrapper(
  redisTemplate: RedisTemplate<String, PaymentRequestInfo>,
  ttl: Duration
) : RedisTemplateWrapper<PaymentRequestInfo>(redisTemplate = redisTemplate, "keys", ttl) {
  override fun getKeyFromEntity(value: PaymentRequestInfo): String =
    "${value.id.fiscalCode}${value.id.noticeId}"
}
