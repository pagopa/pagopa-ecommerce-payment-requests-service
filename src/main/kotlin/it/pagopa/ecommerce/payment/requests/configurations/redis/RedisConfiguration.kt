package it.pagopa.ecommerce.payment.requests.configurations.redis

import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdReadingByteConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdReadingStringConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdWritingByteConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdWritingStringConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.convert.RedisCustomConversions

@Configuration
class RedisConfiguration {

    @Bean
    fun redisCustomConversions(
        rptIdReadingByteConverter: RptIdReadingByteConverter, rptIdReadingStringConverter: RptIdReadingStringConverter,
        rptIdWritingByteConverter: RptIdWritingByteConverter, rptIdWritingStringConverter: RptIdWritingStringConverter,
    ) = RedisCustomConversions(
        listOf(
            rptIdReadingByteConverter, rptIdReadingStringConverter,
            rptIdWritingByteConverter, rptIdWritingStringConverter,
        )
    )
}