package it.pagopa.ecommerce.payment.requests.repositories.redistemplate

import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Mono

abstract class ReactiveRedisTemplateWrapper<V : Any>(
  val reactiveRedisTemplate: ReactiveRedisTemplate<String, V>,
  private val keyspace: String,
  private val ttl: Duration
) {

  fun save(value: V): Mono<Boolean> {
    val key = "$keyspace:${getKeyFromEntity(value)}"
    return reactiveRedisTemplate.opsForValue().set(key, value, ttl)
  }

  fun findById(key: String): Mono<V> {
    return reactiveRedisTemplate.opsForValue()["$keyspace:$key"]
  }

  protected abstract fun getKeyFromEntity(value: V): String
}
