package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@ReadingConverter
class RptIdReadingByteConverter : Converter<ByteArray, RptId> {
    override fun convert(source: ByteArray): RptId {
        return RptId(String(source, StandardCharsets.UTF_8))
    }


}