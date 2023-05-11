package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

abstract class RedisTemplateWrapper<V>(
  private val redisTemplate: RedisTemplate<String, V>,
  private val keyspace: String,
  private val ttl: Duration
) {

  fun setValue(value: V) {
    redisTemplate.opsForValue().set("$keyspace:${getKeyFromEntity(value)}", value!!, ttl)
  }

  fun getValue(key: String): V? = redisTemplate.opsForValue().get("$keyspace:$key")

  protected abstract fun getKeyFromEntity(value: V): String
}
