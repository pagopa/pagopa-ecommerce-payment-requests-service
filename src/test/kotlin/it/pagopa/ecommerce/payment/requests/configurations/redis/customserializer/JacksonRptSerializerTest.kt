package it.pagopa.ecommerce.payment.requests.configurations.redis.customserializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class JacksonRptSerializerTest {

  private val jacksonRptSerializer = JacksonRptSerializer()

  private val jsonGenerator: JsonGenerator = mock()

  private val serializerProvider: SerializerProvider = mock()

  @Test
  fun `Should serialize RPT id successfully`() {
    val rptId = "01234567891999999999999999999"
    jacksonRptSerializer.serialize(RptId(rptId), jsonGenerator, serializerProvider)
    verify(jsonGenerator, times(1)).writeString(rptId)
  }
}
