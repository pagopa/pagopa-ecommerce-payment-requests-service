package it.pagopa.ecommerce.payment.requests.mdcutilities

import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils.AttributeKeys
import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils.Companion.enrichContextForEvent
import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils.Companion.loggerTracingUtils
import java.util.EnumMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.slf4j.Logger
import org.slf4j.MDC
import reactor.util.context.Context

class LogTracingUtilsTest {

  private lateinit var mockLogger: Logger

  @BeforeEach
  fun setUp() {
    mockLogger = mock(Logger::class.java)
    MDC.clear()
  }

  @AfterEach
  fun tearDown() {
    MDC.clear()
  }

  @Test
  fun testLogInfo_withSuccessAndAttributes() {
    // Arrange
    val attributes =
      mapOf(
        AttributeKeys.EVENT_ACTION to "test-action",
        AttributeKeys.CTX_TRANSACTION_ID to "12345",
      )

    doAnswer {
        assertEquals("test-action", MDC.get("event_action"))
        assertEquals("12345", MDC.get("ctx_transaction_id"))
        assertEquals("success", MDC.get("event_outcome"))
        null
      }
      .`when`(mockLogger)
      .info(anyString())

    // Act
    loggerTracingUtils().attributes(attributes).success().logInfo(mockLogger, "Test info message")

    // Assert
    verify(mockLogger).info("Test info message")
    assertNull(MDC.get("event_action"), "MDC should be cleaned up after logging")
    assertNull(MDC.get("correlation.id"))
    assertNull(MDC.get("event_outcome"))
  }

  @Test
  fun testLogError_withExceptionAndStackTrace() {
    // Arrange
    val testException = RuntimeException("Something went wrong")

    doAnswer {
        assertEquals("failure", MDC.get("event_outcome"))
        assertEquals(RuntimeException::class.java.name, MDC.get("error_type"))
        assertEquals("Something went wrong", MDC.get("error_message"))
        assertNotNull(MDC.get("error_stack_trace"))
        assertTrue(MDC.get("error_stack_trace").contains("Something went wrong"))
        null
      }
      .`when`(mockLogger)
      .error(anyString())

    // Act
    loggerTracingUtils()
      .failure()
      .logErrorWithStackTrace(mockLogger, testException, "Test error message")

    // Assert
    verify(mockLogger).error("Test error message")
    assertNull(MDC.get("error_type"))
    assertNull(MDC.get("error_stack_trace"))
  }

  @Test
  fun testLogDebug_withDetailsAndDependency() {
    // Arrange
    val details = mapOf("userId" to "u-123", "retryCount" to "3")

    doAnswer {
        val mdcDetails = MDC.get("ctx_details")
        assertNotNull(mdcDetails)
        assertTrue(mdcDetails.contains("\"userId\":\"u-123\""))
        assertTrue(mdcDetails.contains("\"retryCount\":\"3\""))
        assertTrue(mdcDetails.contains("\"dependency\":\"my-dependency\""))
        null
      }
      .`when`(mockLogger)
      .debug(anyString())

    // Act
    loggerTracingUtils()
      .details(details)
      .dependency("my-dependency")
      .logDebug(mockLogger, "Test debug message")

    // Assert
    verify(mockLogger).debug("Test debug message")
    assertNull(MDC.get("ctx_details"))
  }

  @Test
  fun testLogWarn_basic() {
    // Arrange
    doAnswer {
        val contextMap = MDC.getCopyOfContextMap()
        assertTrue(
          contextMap == null || contextMap.isEmpty(),
          "MDC should be empty since no attributes were added",
        )
        null
      }
      .`when`(mockLogger)
      .warn(anyString())

    // Act
    loggerTracingUtils().logWarn(mockLogger, "Warning message")

    // Assert
    verify(mockLogger).warn("Warning message")
  }

  @Test
  fun testLogTrace_basic() {
    // Act
    loggerTracingUtils().logTrace(mockLogger, "Trace message")

    // Assert
    verify(mockLogger).trace("Trace message")
  }

  @Test
  fun testErrorWithoutMessage() {
    // Arrange
    val exceptionNoMessage = Exception()

    doAnswer {
        assertEquals(Exception::class.java.name, MDC.get("error_type"))
        assertEquals("{errorMessage-not-found}", MDC.get("error_message"))
        null
      }
      .`when`(mockLogger)
      .error(anyString())

    // Act
    loggerTracingUtils().logError(mockLogger, exceptionNoMessage, "Error happened")

    // Assert
    verify(mockLogger).error("Error happened")
  }

  @Test
  fun testNullAttributeKeysAndValuesAreIgnored() {
    // Arrange
    val attributes =
      EnumMap<AttributeKeys, String>(AttributeKeys::class.java).apply {
        put(AttributeKeys.CTX_TRANSACTION_ID, null)
      }

    doAnswer {
        assertNull(MDC.get("ctx_transaction_id"))
        null
      }
      .`when`(mockLogger)
      .info(anyString())

    // Act
    loggerTracingUtils().attributes(attributes).logInfo(mockLogger, "Testing nulls")

    // Assert
    verify(mockLogger).info("Testing nulls")
  }

  @Test
  fun shouldReturnSameContextWhenTracingEntriesAreNull() {
    // Arrange
    val reactorContext = Context.of("existing-key", "existing-value")

    // Act
    val enrichedContext = enrichContextForEvent(null, reactorContext)

    // Assert
    assertSame(reactorContext, enrichedContext)
    assertEquals("existing-value", enrichedContext.get<String>("existing-key"))
  }

  @Test
  fun shouldPreserveExistingContextEntriesWhenEnriching() {
    // Arrange
    val existingContext = Context.of("pre-existing-key", "pre-existing-value")
    val tracingEntries = mapOf(AttributeKeys.EVENT_ACTION to "event_action")

    // Act
    val enrichedContext = enrichContextForEvent(tracingEntries, existingContext)

    // Assert
    assertEquals("pre-existing-value", enrichedContext.get<String>("pre-existing-key"))
    assertEquals("event_action", enrichedContext.get<String>(AttributeKeys.EVENT_ACTION.key))
  }

  @Test
  fun shouldEnrichContextUsingProvidedAndDefaultValues() {
    // Arrange
    val tracingEntries =
      EnumMap<AttributeKeys, String>(AttributeKeys::class.java).apply {
        put(AttributeKeys.EVENT_ACTION, "event_action")
        put(AttributeKeys.CTX_TRANSACTION_ID, null)
      }

    // Act
    val enrichedContext = enrichContextForEvent(tracingEntries, Context.empty())

    // Assert
    assertEquals("event_action", enrichedContext.get<String>("event_action"))
    assertEquals("{transactionId-not-found}", enrichedContext.get<String>("ctx_transaction_id"))
  }
}
