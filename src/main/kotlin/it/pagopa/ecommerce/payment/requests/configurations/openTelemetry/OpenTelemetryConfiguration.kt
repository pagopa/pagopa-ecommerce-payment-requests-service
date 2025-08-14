package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry

import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.lettuce.core.tracing.Tracing
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.lettuce.v5_1.LettuceTelemetry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryConfiguration {

  @Bean
  fun agentOpenTelemetrySDKInstance(): OpenTelemetry {
    return GlobalOpenTelemetry.get()
  }

  @Bean
  fun openTelemetryTracer(openTelemetry: OpenTelemetry): Tracer {
    return openTelemetry.getTracer("pagopa-ecommerce-payment-requests-service")
  }

  // Manual configuration of Redis instrumentation
  @Bean
  fun otelLettuceClientResources(openTelemetry: OpenTelemetry): ClientResources {
    val telemetry: LettuceTelemetry = LettuceTelemetry.builder(openTelemetry).build()
    val tracing: Tracing = telemetry.newTracing()

    return DefaultClientResources.builder().tracing(tracing).build()
  }
}
