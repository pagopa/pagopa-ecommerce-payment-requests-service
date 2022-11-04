package it.pagopa.ecommerce.payment.requests.configuration.redis

import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId.RptIdReadingByteConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId.RptIdReadingStringConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId.RptIdWritingByteConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId.RptIdWritingStringConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid.UUIDReadingByteConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid.UUIDReadingStringConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid.UUIDWritingByteConverter
import it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid.UUIDWritingStringConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.convert.RedisCustomConversions

@Configuration
class RedisConfiguration {

    @Bean
    fun redisCustomConversions(
        rptIdReadingByteConverter: RptIdReadingByteConverter, rptIdReadingStringConverter: RptIdReadingStringConverter,
        rptIdWritingByteConverter: RptIdWritingByteConverter, rptIdWritingStringConverter: RptIdWritingStringConverter,
        uuidReadingByteConverter: UUIDReadingByteConverter, uuidReadingStringConverter: UUIDReadingStringConverter,
        uuidWritingByteConverter: UUIDWritingByteConverter, uuidWritingStringConverter: UUIDWritingStringConverter
    ) = RedisCustomConversions(
        listOf(
            rptIdReadingByteConverter, rptIdReadingStringConverter,
            rptIdWritingByteConverter, rptIdWritingStringConverter,
            uuidReadingByteConverter, uuidReadingStringConverter,
            uuidWritingByteConverter, uuidWritingStringConverter
        )
    )
}