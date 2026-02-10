package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.v2.CartInfoV2
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

class CartsRedisTemplateWrapperV2(
  reactiveRedisTemplate: ReactiveRedisTemplate<String, CartInfoV2>,
  ttl: Duration
) :
  ReactiveRedisTemplateWrapper<CartInfoV2>(
    reactiveRedisTemplate = reactiveRedisTemplate, "carts", ttl) {
  override fun getKeyFromEntity(value: CartInfoV2): String = value.id.toString()
}
