package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
@WritingConverter
class UUIDWritingByteConverter : Converter<UUID, ByteArray> {
    override fun convert(source: UUID): ByteArray {
        return source.toString().toByteArray(StandardCharsets.UTF_8)
    }
}