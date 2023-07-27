package it.pagopa.ecommerce.payment.requests.utils

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.util.OpenTelemetryUtils
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class OpenTelemetryUtilsTest {

  private val openTelemetryTracer: Tracer = Mockito.mock(Tracer::class.java)

  private val spanBuilder = Mockito.mock(SpanBuilder::class.java)

  private val span: Span = Mockito.mock(Span::class.java)

  private val openTelemetryUtils = OpenTelemetryUtils(openTelemetryTracer)

  @Test
  fun shouldCreateSpanWithAttributes() {
    // prerequisite
    val spanName = "spanName"
    val attributes: Attributes = Attributes.of(AttributeKey.stringKey("key"), "value")
    Mockito.`when`(openTelemetryTracer.spanBuilder(spanName)).thenReturn(spanBuilder)
    Mockito.`when`(spanBuilder.startSpan()).thenReturn(span)
    Mockito.`when`(span.setAllAttributes(Mockito.any())).thenReturn(span)
    // test
    openTelemetryUtils.addSpanWithAttributes(spanName, attributes)
    // assertions
    Mockito.verify(openTelemetryTracer, Mockito.times(1)).spanBuilder(spanName)
    Mockito.verify(span, Mockito.times(1)).setAllAttributes(attributes)
    Mockito.verify(span, Mockito.times(1)).end()
  }

  fun shouldCreateErrorSpanWithAttributes() {
    // prerequisite
    val spanName = "spanName"
    val attributes = Attributes.of(AttributeKey.stringKey("key"), "value")
    Mockito.`when`(openTelemetryTracer.spanBuilder(spanName)).thenReturn(spanBuilder)
    Mockito.`when`(spanBuilder.startSpan()).thenReturn(span)
    Mockito.`when`(span.setAllAttributes(ArgumentMatchers.any())).thenReturn(span)
    // test
    openTelemetryUtils.addErrorSpanWithAttributes(spanName, attributes)
    // assertions
    Mockito.verify(openTelemetryTracer, Mockito.times(1)).spanBuilder(spanName)
    Mockito.verify(span, Mockito.times(1)).setAllAttributes(attributes)
    Mockito.verify(span, Mockito.times(1)).setStatus(StatusCode.ERROR)
    Mockito.verify(span, Mockito.times(1)).end()
  }

  @Test
  fun shouldCreateErrorSpanWithThrowable() {
    // prerequisite
    val spanName = "spanName"
    val throwable: Throwable = RuntimeException("test exception")
    Mockito.`when`(openTelemetryTracer.spanBuilder(spanName)).thenReturn(spanBuilder)
    Mockito.`when`(spanBuilder.startSpan()).thenReturn(span)
    Mockito.`when`(span.setAllAttributes(ArgumentMatchers.any())).thenReturn(span)
    // test
    openTelemetryUtils.addErrorSpanWithException(spanName, throwable)
    // assertions
    Mockito.verify(openTelemetryTracer, Mockito.times(1)).spanBuilder(spanName)
    Mockito.verify(span, Mockito.times(1)).setStatus(StatusCode.ERROR)
    Mockito.verify(span, Mockito.times(1)).recordException(throwable)
    Mockito.verify(span, Mockito.times(1)).end()
  }
}
