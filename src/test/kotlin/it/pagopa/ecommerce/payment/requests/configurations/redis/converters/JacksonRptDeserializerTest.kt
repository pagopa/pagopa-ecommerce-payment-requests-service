package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

class JacksonRptDeserializerTest {

  private val jacksonRptDeserializer = JacksonRptDeserializer()

  private val jsonParser: JsonParser = mock()

  private val deserializationContext: DeserializationContext = mock()

  @Test
  fun `Should deserialize RPT ID successfully`() {
    val rptId = "01234567891999999999999999999"
    given(jsonParser.valueAsString).willReturn(rptId)
    val deserializedRptId = jacksonRptDeserializer.deserialize(jsonParser, deserializationContext)
    assertEquals(RptId(rptId), deserializedRptId)
  }
}
