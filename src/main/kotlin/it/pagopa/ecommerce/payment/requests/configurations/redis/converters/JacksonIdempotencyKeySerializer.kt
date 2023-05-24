package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import it.pagopa.ecommerce.payment.requests.domain.IdempotencyKey

class JacksonIdempotencyKeySerializer : JsonSerializer<IdempotencyKey>() {
  override fun serialize(
    value: IdempotencyKey,
    gen: JsonGenerator,
    serializers: SerializerProvider
  ) = gen.writeString(value.rawValue)
}
