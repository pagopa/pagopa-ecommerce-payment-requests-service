package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.util

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Open telemetry utility class used to create spans inside the current transaction with custom
 * arguments
 *
 * @see Tracer
 * @see Span
 */
@Component
class OpenTelemetryUtils(
  @Autowired private val openTelemetryTracer: Tracer,
) {

  /**
   * Add span to the current transactions with the input arguments and name
   *
   * @param spanName
   * - span name
   * @param attributes
   * - span attributes
   */
  fun addSpanWithAttributes(spanName: String?, attributes: Attributes?) {
    val span = openTelemetryTracer.spanBuilder(spanName!!).startSpan()
    span.setAllAttributes(attributes!!)
    span.end()
  }

  /**
   * Add an error span to the current transaction with the input arguments and name
   *
   * @param spanName
   * - error span name
   * @param attributes
   * - error span attributes
   */
  fun addErrorSpanWithAttributes(spanName: String?, attributes: Attributes?) {
    val span = openTelemetryTracer.spanBuilder(spanName!!).startSpan()
    span.setAllAttributes(attributes!!)
    span.setStatus(StatusCode.ERROR)
    span.end()
  }

  /**
   * Add an error span to the current transaction with the input name and throwable stacktrace
   *
   * @param spanName
   * - error span name
   * @param throwable
   * - error cause Throwable
   */
  fun addErrorSpanWithException(spanName: String?, throwable: Throwable?) {
    val span = openTelemetryTracer.spanBuilder(spanName!!).startSpan()
    span.setStatus(StatusCode.ERROR)
    span.recordException(throwable!!)
    span.end()
  }
}
