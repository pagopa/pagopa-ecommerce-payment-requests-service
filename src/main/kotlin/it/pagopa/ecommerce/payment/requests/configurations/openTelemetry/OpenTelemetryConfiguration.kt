package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
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
}
