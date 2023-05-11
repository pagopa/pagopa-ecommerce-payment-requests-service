package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class JacksonRptConverterTests {

  private val jacksonRptDeserializer = JacksonRptDeserializer()

  private val jsonParser: JsonParser = mock()

  private val deserializationContext: DeserializationContext = mock()

  private val jacksonRptSerializer = JacksonRptSerializer()

  private val jsonGenerator: JsonGenerator = mock()

  private val serializerProvider: SerializerProvider = mock()

  @Test
  fun `Should deserialize RPT ID successfully`() {
    val rptId = "01234567891999999999999999999"
    given(jsonParser.valueAsString).willReturn(rptId)
    val deserializedRptId = jacksonRptDeserializer.deserialize(jsonParser, deserializationContext)
    Assertions.assertEquals(RptId(rptId), deserializedRptId)
  }

  @Test
  fun `Should serialize RPT id successfully`() {
    val rptId = "01234567891999999999999999999"
    jacksonRptSerializer.serialize(RptId(rptId), jsonGenerator, serializerProvider)
    verify(jsonGenerator, times(1)).writeString(rptId)
  }
}
