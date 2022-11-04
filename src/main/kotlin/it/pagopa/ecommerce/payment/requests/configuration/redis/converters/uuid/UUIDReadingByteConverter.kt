package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.uuid

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
@ReadingConverter
class UUIDReadingByteConverter : Converter<ByteArray, UUID> {
    override fun convert(source: ByteArray): UUID {
        return UUID.fromString(String(source, StandardCharsets.UTF_8))
    }


}