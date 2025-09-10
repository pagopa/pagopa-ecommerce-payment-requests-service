package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

class CartsRedisTemplateWrapper(
  reactiveRedisTemplate: ReactiveRedisTemplate<String, CartInfo>,
  ttl: Duration
) :
  ReactiveRedisTemplateWrapper<CartInfo>(
    reactiveRedisTemplate = reactiveRedisTemplate, "carts", ttl) {
  override fun getKeyFromEntity(value: CartInfo): String = value.id.toString()
}
