package it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.stereotype.Component

@Component
@ReadingConverter
class RptIdReadingStringConverter : Converter<String, RptId> {
  override fun convert(source: String): RptId {
    return RptId(source)
  }
}
