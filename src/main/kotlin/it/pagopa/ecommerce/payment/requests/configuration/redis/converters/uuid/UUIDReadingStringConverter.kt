package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component
import java.util.*

@Component
@ReadingConverter
class UUIDReadingStringConverter : Converter<String, UUID> {
    override fun convert(source: String): UUID {
        return UUID.fromString(source)
    }
}