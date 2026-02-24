package it.pagopa.ecommerce.payment.requests.configurations.redis

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.JacksonIdempotencyKeyDeserializer
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.JacksonIdempotencyKeySerializer
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.JacksonRptDeserializer
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.JacksonRptSerializer
import it.pagopa.ecommerce.payment.requests.domain.IdempotencyKey
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.CartsRedisTemplateWrapper
import it.pagopa.ecommerce.payment.requests.repositories.redistemplate.PaymentRequestsRedisTemplateWrapper
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfiguration {

  @Bean
  fun paymentRequestsInfoRedisTemplate(
    reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
  ): PaymentRequestsRedisTemplateWrapper {
    val keySerializer = StringRedisSerializer()
    val valueSerializer = buildJackson2RedisSerializer(PaymentRequestInfo::class.java)

    val serializationContext =
      RedisSerializationContext.newSerializationContext<String, PaymentRequestInfo>(keySerializer)
        .value(valueSerializer)
        .build()

    val paymentRequestInfoTemplate =
      ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext)

    return PaymentRequestsRedisTemplateWrapper(paymentRequestInfoTemplate, Duration.ofMinutes(15))
  }

  @Bean
  fun cartsRedisTemplate(
    reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory
  ): CartsRedisTemplateWrapper {
    val keySerializer = StringRedisSerializer()
    val valueSerializer = buildJackson2RedisSerializer(CartInfo::class.java)

    val serializationContext =
      RedisSerializationContext.newSerializationContext<String, CartInfo>(keySerializer)
        .value(valueSerializer)
        .build()

    val cartInfoTemplate =
      ReactiveRedisTemplate(reactiveRedisConnectionFactory, serializationContext)

    return CartsRedisTemplateWrapper(cartInfoTemplate, Duration.ofMinutes(10))
  }

  private fun <T> buildJackson2RedisSerializer(clazz: Class<T>): Jackson2JsonRedisSerializer<T> {
    val jacksonObjectMapper = jacksonObjectMapper()
    val rptSerializationModule = SimpleModule()
    rptSerializationModule.addSerializer(RptId::class.java, JacksonRptSerializer())
    rptSerializationModule.addDeserializer(RptId::class.java, JacksonRptDeserializer())
    rptSerializationModule.addSerializer(
      IdempotencyKey::class.java, JacksonIdempotencyKeySerializer())
    rptSerializationModule.addDeserializer(
      IdempotencyKey::class.java, JacksonIdempotencyKeyDeserializer())

    jacksonObjectMapper.registerModule(rptSerializationModule)
    jacksonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

    return Jackson2JsonRedisSerializer(jacksonObjectMapper, clazz)
  }
}
