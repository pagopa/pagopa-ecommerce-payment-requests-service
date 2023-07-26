package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.util;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryUtils {

    private final Tracer openTelemetryTracer;

    @Autowired
    public OpenTelemetryUtils(Tracer openTelemetryTracer) {
        this.openTelemetryTracer = openTelemetryTracer;
    }

    public void addSpanWithAttributes(
            String spanName,
            Attributes attributes
    ) {

        Span span = openTelemetryTracer.spanBuilder(spanName).startSpan();
        span.setAllAttributes(attributes);
        span.end();

    }

    public void addErrorSpanWithError(
            String spanName,
            Throwable throwable
    ) {
        Span span = openTelemetryTracer.spanBuilder(spanName).startSpan();
        span
                .setStatus(StatusCode.ERROR)
                .recordException(throwable);
        span.end();
    }
}