package it.pagopa.ecommerce.payment.requests.configurations.opentelemetry

import io.opentelemetry.api.OpenTelemetry
import it.pagopa.ecommerce.payment.requests.configurations.openTelemetry.OpenTelemetryConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OpenTelemetryConfigurationTests {
  private val openTelemetryConfiguration = OpenTelemetryConfiguration()

  @Test
  fun `Should get openTelemetry instance`() {
    val openTelemetry = openTelemetryConfiguration.agentOpenTelemetrySDKInstance()
    Assertions.assertNotNull(openTelemetry)
    Assertions.assertTrue(openTelemetry is OpenTelemetry)
  }

  @Test
  fun `Should get tracer instance`() {
    val openTelemetry = openTelemetryConfiguration.agentOpenTelemetrySDKInstance()
    val tracer = openTelemetryConfiguration.openTelemetryTracer(openTelemetry)
    Assertions.assertNotNull(tracer)
    Assertions.assertEquals(
      tracer, openTelemetry.getTracer("pagopa-ecommerce-payment-requests-service"))
  }
}
