package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

abstract class RedisTemplateWrapper<V>(
  val redisTemplate: RedisTemplate<String, V>,
  private val keyspace: String,
  private val ttl: Duration
) {

  fun save(value: V) {
    redisTemplate.opsForValue()["$keyspace:${getKeyFromEntity(value)}", value!!] = ttl
  }

  fun findById(key: String): V? = redisTemplate.opsForValue()["$keyspace:$key"]

  protected abstract fun getKeyFromEntity(value: V): String
}
