package it.pagopa.ecommerce.payment.requests.configurations.redis

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.ecommerce.payment.requests.configurations.redis.customserializer.JacksonRptDeserializer
import it.pagopa.ecommerce.payment.requests.configurations.redis.customserializer.JacksonRptSerializer
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.PaymentRequestsRedisTemplateWrapper
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfiguration {

  @Bean
  fun paymentRequestsInfoRedisTemplate(
    redisConnectionFactory: RedisConnectionFactory
  ): PaymentRequestsRedisTemplateWrapper {
    val paymentRequestInfoTemplate = RedisTemplate<String, PaymentRequestInfo>()
    paymentRequestInfoTemplate.setConnectionFactory(redisConnectionFactory)
    val jackson2JsonRedisSerializer = buildJackson2RedisSerializer(PaymentRequestInfo::class.java)
    paymentRequestInfoTemplate.valueSerializer = jackson2JsonRedisSerializer
    paymentRequestInfoTemplate.keySerializer = StringRedisSerializer()
    paymentRequestInfoTemplate.afterPropertiesSet()
    return PaymentRequestsRedisTemplateWrapper(paymentRequestInfoTemplate, Duration.ofMinutes(10))
  }

  @Bean
  fun cartsRedisTemplate(
    redisConnectionFactory: RedisConnectionFactory
  ): CartsRedisTemplateWrapper {
    val cartTemplate = RedisTemplate<String, CartInfo>()
    cartTemplate.setConnectionFactory(redisConnectionFactory)
    val jackson2JsonRedisSerializer = buildJackson2RedisSerializer(CartInfo::class.java)
    cartTemplate.valueSerializer = jackson2JsonRedisSerializer
    cartTemplate.keySerializer = StringRedisSerializer()
    cartTemplate.afterPropertiesSet()
    return CartsRedisTemplateWrapper(cartTemplate, Duration.ofMinutes(10))
  }

  private fun <T> buildJackson2RedisSerializer(clazz: Class<T>): Jackson2JsonRedisSerializer<T> {
    val jackson2JsonRedisSerializer = Jackson2JsonRedisSerializer(clazz)
    val jacksonObjectMapper = jacksonObjectMapper()
    val rptSerializationModule = SimpleModule()
    rptSerializationModule.addSerializer(RptId::class.java, JacksonRptSerializer())
    rptSerializationModule.addDeserializer(RptId::class.java, JacksonRptDeserializer())
    jacksonObjectMapper.registerModule(rptSerializationModule)
    jackson2JsonRedisSerializer.setObjectMapper(jacksonObjectMapper)
    return jackson2JsonRedisSerializer
  }
}
