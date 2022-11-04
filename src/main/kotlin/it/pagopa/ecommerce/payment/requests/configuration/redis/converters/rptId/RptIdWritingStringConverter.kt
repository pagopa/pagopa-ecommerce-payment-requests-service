package it.pagopa.ecommerce.payment.requests.configuration.redis.converters.rptId

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
class RptIdWritingStringConverter : Converter<RptId, String> {
    override fun convert(source: RptId): String {
        return source.value
    }
}