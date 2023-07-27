package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.util

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OpenTelemetryUtils(
  @Autowired private val openTelemetryTracer: Tracer,
) {

  fun addSpanWithAttributes(spanName: String?, attributes: Attributes?) {
    val span = openTelemetryTracer.spanBuilder(spanName!!).startSpan()
    span.setAllAttributes(attributes!!)
    span.end()
  }
}
