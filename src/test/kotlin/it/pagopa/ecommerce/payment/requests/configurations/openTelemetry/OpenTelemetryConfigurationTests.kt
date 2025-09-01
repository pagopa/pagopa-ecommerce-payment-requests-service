package it.pagopa.ecommerce.payment.requests.configurations.openTelemetry

import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.opentelemetry.api.OpenTelemetry
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

  @Test
  fun testBeanCreationProvidesTracingClientResources() {
    val openTelemetry = openTelemetryConfiguration.agentOpenTelemetrySDKInstance()
    val clientResources: ClientResources =
      openTelemetryConfiguration.otelLettuceClientResources(openTelemetry)

    Assertions.assertNotNull(clientResources)
    Assertions.assertTrue(clientResources is DefaultClientResources)

    val defaultResources = clientResources as DefaultClientResources
    val tracing = defaultResources.tracing()
    Assertions.assertNotNull(tracing)
  }
}
