package it.pagopa.ecommerce.payment.requests.configurations.redis.converters

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import it.pagopa.ecommerce.payment.requests.domain.IdempotencyKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class JacksonIdempotencyConverterTests {

  private val jacksonIdempotencyKeyDeserializer = JacksonIdempotencyKeyDeserializer()

  private val jsonParser: JsonParser = mock()

  private val deserializationContext: DeserializationContext = mock()

  private val jacksonIdempotencyKeySerializer = JacksonIdempotencyKeySerializer()

  private val jsonGenerator: JsonGenerator = mock()

  private val serializerProvider: SerializerProvider = mock()

  @Test
  fun `Should deserialize idempotency key successfully`() {
    val idempotencyKey = "32009090901_aabbccddee"
    given(jsonParser.valueAsString).willReturn(idempotencyKey)
    val deserializedIdempotencyKey =
      jacksonIdempotencyKeyDeserializer.deserialize(jsonParser, deserializationContext)
    assertEquals(IdempotencyKey(idempotencyKey), deserializedIdempotencyKey)
  }

  @Test
  fun `Should serialize idempotency key successfully`() {
    val idempotencyKey = "32009090901_aabbccddee"
    jacksonIdempotencyKeySerializer.serialize(
      IdempotencyKey(idempotencyKey), jsonGenerator, serializerProvider)
    verify(jsonGenerator, times(1)).writeString(idempotencyKey)
  }
}
