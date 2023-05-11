package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import it.pagopa.ecommerce.payment.requests.domain.RptId

class JacksonRptDeserializer : JsonDeserializer<RptId>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): RptId =
    RptId(p.valueAsString)
}
