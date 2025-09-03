package it.pagopa.ecommerce.payment.requests.configurations.redis

import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.StringRedisSerializer

class RedisConfigurationTest {
  private val redisConfiguration = RedisConfiguration()

  private val redisConnectionFactory: RedisConnectionFactory = mock()

  @Test
  fun `Should build PaymentRequestInfo Redis template successfully`() {
    val paymentRequestsRedisTemplateWrapper =
      redisConfiguration.paymentRequestsInfoRedisTemplate(redisConnectionFactory)
    val redisTemplate = paymentRequestsRedisTemplateWrapper.redisTemplate
    assertEquals(StringRedisSerializer::class.java, redisTemplate.keySerializer.javaClass)
    assertTrue(redisTemplate.valueSerializer.canSerialize(PaymentRequestInfo::class.java))
  }

  @Test
  fun `Should build Carts Redis template successfully`() {
    val cartsRedisTemplateWrapper = redisConfiguration.cartsRedisTemplate(redisConnectionFactory)
    val redisTemplate = cartsRedisTemplateWrapper.redisTemplate
    assertEquals(StringRedisSerializer::class.java, redisTemplate.keySerializer.javaClass)
    assertTrue(redisTemplate.valueSerializer.canSerialize(CartInfo::class.java))
  }
}
