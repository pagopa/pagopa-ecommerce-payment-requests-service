package it.pagopa.ecommerce.payment.requests.repositories.redistemplate.v1

import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.ReactiveRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.repositories.v1.CartInfo
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
