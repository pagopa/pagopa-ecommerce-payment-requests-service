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
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import java.nio.ByteBuffer
import java.util.*

class RedisConfigurationTest {
  private val redisConfiguration = RedisConfiguration()

  private val redisConnectionFactory: ReactiveRedisConnectionFactory = mock()

    @Test
    fun `Should build PaymentRequestInfo Redis template successfully`() {
        val paymentRequestsRedisTemplateWrapper = redisConfiguration.paymentRequestsInfoRedisTemplate(redisConnectionFactory)
        assertNotNull(paymentRequestsRedisTemplateWrapper)

        val reactiveRedisTemplate = paymentRequestsRedisTemplateWrapper.reactiveRedisTemplate
        assertNotNull(reactiveRedisTemplate)

        val paymentRequestInfo = PaymentRequestInfo(
            RptId("77777777777302016432223611415"),
            "77777777777",
            "companyName",
            "Pagamento di Test",
            12000,
            "2021-07-31",
            "1fb8539bdbc94123849a21be8eead8dd",
            "2021-07-31",
            null,
            null,
            null,
            null
        )

        val actual: ByteBuffer = reactiveRedisTemplate.getSerializationContext().getValueSerializationPair().getWriter()
            .write(paymentRequestInfo)

        val expected: ByteBuffer? = ByteBuffer
            .wrap(buildJackson2RedisSerializer(PaymentRequestInfo::class.java).serialize(paymentRequestInfo))

        assertEquals(actual, expected)
    }

    @Test
    fun `Should build Carts Redis template successfully`() {
        val cartsRedisTemplateWrapper = redisConfiguration.cartsRedisTemplate(redisConnectionFactory)
        assertNotNull(cartsRedisTemplateWrapper)

        val reactiveRedisTemplate = cartsRedisTemplateWrapper.reactiveRedisTemplate
        assertNotNull(reactiveRedisTemplate)

        val cartInfo = CartInfo(
            UUID.randomUUID(),
            listOf(PaymentInfo(
                RptId("77777777777302016432223611415"),
                "description",
                10000,
                "companyName"
            )),
            "idCartExample",
            ReturnUrls(
                "www.comune.di.prova.it/pagopa/success.html",
                "www.comune.di.prova.it/pagopa/cancel.html",
                "www.comune.di.prova.it/pagopa/error.html",
            ),
            "my_email@mail.it"
        )

        val actual: ByteBuffer = reactiveRedisTemplate.getSerializationContext().getValueSerializationPair().getWriter()
            .write(cartInfo)

        val expected: ByteBuffer? = ByteBuffer
            .wrap(buildJackson2RedisSerializer(CartInfo::class.java).serialize(cartInfo))

        assertEquals(actual, expected)
    }

    private fun <T> buildJackson2RedisSerializer(clazz: Class<T>): Jackson2JsonRedisSerializer<T> {
        val jacksonObjectMapper = jacksonObjectMapper()
        val rptSerializationModule = SimpleModule()
        rptSerializationModule.addSerializer(RptId::class.java, JacksonRptSerializer())
        rptSerializationModule.addDeserializer(RptId::class.java, JacksonRptDeserializer())
        rptSerializationModule.addSerializer(IdempotencyKey::class.java, JacksonIdempotencyKeySerializer())
        rptSerializationModule.addDeserializer(IdempotencyKey::class.java, JacksonIdempotencyKeyDeserializer())

        jacksonObjectMapper.registerModule(rptSerializationModule)
        jacksonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        return Jackson2JsonRedisSerializer(jacksonObjectMapper, clazz)
    }
}
