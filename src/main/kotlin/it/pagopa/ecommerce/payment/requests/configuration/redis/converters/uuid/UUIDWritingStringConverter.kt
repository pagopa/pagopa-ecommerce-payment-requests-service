package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component
import java.util.*

@Component
@WritingConverter
class UUIDWritingStringConverter : Converter<UUID, String> {
    override fun convert(source: UUID): String {
        return source.toString()
    }
}