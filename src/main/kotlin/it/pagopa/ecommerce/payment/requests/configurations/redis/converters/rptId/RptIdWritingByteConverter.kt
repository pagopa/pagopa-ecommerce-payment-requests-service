package it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId

import it.pagopa.ecommerce.payment.requests.domain.RptId
import java.nio.charset.StandardCharsets
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component

@Component
@WritingConverter
class RptIdWritingByteConverter : Converter<RptId, ByteArray> {
  override fun convert(source: RptId): ByteArray {
    return source.value.toByteArray(StandardCharsets.UTF_8)
  }
}
