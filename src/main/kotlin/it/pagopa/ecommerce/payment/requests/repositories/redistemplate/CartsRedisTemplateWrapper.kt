package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

class CartsRedisTemplateWrapper(redisTemplate: RedisTemplate<String, CartInfo>, ttl: Duration) :
  RedisTemplateWrapper<CartInfo>(redisTemplate = redisTemplate, "carts", ttl) {
  override fun getKeyFromEntity(value: CartInfo): String = value.id.toString()
}
