package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import it.pagopa.ecommerce.payment.requests.domain.RptId

class JacksonRptSerializer : JsonSerializer<RptId>() {
  override fun serialize(value: RptId, gen: JsonGenerator, serializers: SerializerProvider) =
    gen.writeString(value.value)
}
