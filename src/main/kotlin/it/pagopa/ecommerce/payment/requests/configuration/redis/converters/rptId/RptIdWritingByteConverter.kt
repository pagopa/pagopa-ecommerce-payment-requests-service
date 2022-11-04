package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@WritingConverter
class RptIdWritingByteConverter : Converter<RptId, ByteArray> {
    override fun convert(source: RptId): ByteArray {
        return source.value.toByteArray(StandardCharsets.UTF_8)
    }
}