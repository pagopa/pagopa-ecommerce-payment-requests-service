package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import it.pagopa.ecommerce.payment.requests.domain.IdempotencyKey

class JacksonIdempotencyKeyDeserializer : JsonDeserializer<IdempotencyKey>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IdempotencyKey =
    IdempotencyKey(p.valueAsString)
}
