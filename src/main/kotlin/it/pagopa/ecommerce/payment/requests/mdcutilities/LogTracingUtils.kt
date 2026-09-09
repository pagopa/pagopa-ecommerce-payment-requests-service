package it.pagopa.ecommerce.payment.requests.mdcutilities

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level
import reactor.util.context.Context

/**
 * Utility class for structured logging utilizing the Fluent Builder pattern.
 *
 * This class facilitates the population of the SLF4J Mapped Diagnostic Context (MDC) with
 * predefined attributes, custom details, and error information. It ensures that MDC keys are safely
 * added before logging and properly cleaned up immediately after the log is emitted, preventing
 * context leaks in concurrent environments.
 */

/** Utility class for structured logging utilizing the Fluent Builder pattern. */
class LogTracingUtils private constructor() {
  private var outcome: String? = null
  private var message: String? = null
  private var error: Throwable? = null
  private var stackTrace: String? = null
  private var attributes: Map<AttributeKeys, String> = EnumMap(AttributeKeys::class.java)
  private val details = mutableMapOf<String, String?>()
  private var logger: Logger? = null

  private val mdcKeys = mutableListOf<String>()

  enum class AttributeKeys(val key: String, val defaultValue: String) {
    EVENT_ACTION("event_action", "{eventAction-not-found}"),
    CTX_TRANSACTION_ID("ctx_transaction_id", "{transactionId-not-found}"),
    CTX_AUTHORIZATION_REQUEST_ID(
      "ctx_authorization_request_id",
      "{authorizationRequestId-not-found}",
    ),
    CTX_RPT_IDS("ctx_rpt_ids", "{rptIds-not-found}"),
    CTX_WALLET_ID("ctx_wallet_id", "{walletId-not-found}"),
    CTX_EVENT_CODE("ctx_event_code", "{eventCode-not-found}"),
    CTX_EVENT_ID("ctx_event_id", "{eventId-not-found}"),
  }

  private enum class AttributeKeysPrivate(val key: String, val defaultValue: String) {
    CTX_DETAILS("ctx_details", "{details-not-found}"),
    EVENT_OUTCOME("event_outcome", "{eventOutcome-not-found}"),
    DEPENDENCY("dependency", "{dependency-not-found}"),
    ERROR_TYPE("error.type", "{errorType-not-found}"),
    ERROR_MESSAGE("error.message", "{errorMessage-not-found}"),
    ERROR_STACK_TRACE("error.stack_trace", "{errorStackTrace-not-found}"),
  }

  fun attributes(attributes: Map<AttributeKeys, String>) = apply { this.attributes = attributes }

  fun details(details: Map<String, String?>) = apply { this.details.putAll(details) }

  fun dependency(dependency: String) = apply {
    this.details[AttributeKeysPrivate.DEPENDENCY.key] = dependency
  }

  fun success() = apply { this.outcome = SUCCESS }

  fun failure() = apply { this.outcome = FAILURE }

  private fun addMdcKey(key: String, value: String?) {
    value?.let {
      MDC.put(key, it)
      mdcKeys.add(key)
    }
  }

  fun logInfo(logger: Logger, message: String) = log(logger, Level.INFO, message)

  fun logDebug(logger: Logger, message: String) = log(logger, Level.DEBUG, message)

  fun logWarn(logger: Logger, message: String) = log(logger, Level.WARN, message)

  fun logTrace(logger: Logger, message: String) = log(logger, Level.TRACE, message)

  fun logError(logger: Logger, error: Throwable, message: String) =
    apply { this.error = error }.log(logger, Level.ERROR, message)

  fun logErrorWithStackTrace(logger: Logger, error: Throwable, message: String) =
    apply {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        this.stackTrace = sw.toString()
      }
      .logError(logger, error, message)

  private fun log(logger: Logger, loggerLevel: Level, message: String) {
    this.logger = logger
    this.message = message

    try {
      attributes.forEach { (key, value) -> addMdcKey(key.key, value) }

      if (details.isNotEmpty()) {
        addMdcKey(AttributeKeysPrivate.CTX_DETAILS.key, serializeDetailsToMdcMap(details))
      }

      addMdcKey(AttributeKeysPrivate.EVENT_OUTCOME.key, outcome)

      error?.let { err ->
        addMdcKey(AttributeKeysPrivate.ERROR_TYPE.key, err.javaClass.name)
        addMdcKey(
          AttributeKeysPrivate.ERROR_MESSAGE.key,
          err.message ?: AttributeKeysPrivate.ERROR_MESSAGE.defaultValue,
        )
      }

      addMdcKey(AttributeKeysPrivate.ERROR_STACK_TRACE.key, stackTrace)

      when (loggerLevel) {
        Level.INFO -> logger.info(message)
        Level.WARN -> logger.warn(message)
        Level.DEBUG -> logger.debug(message)
        Level.TRACE -> logger.trace(message)
        Level.ERROR -> logger.error(message)
      }
    } finally {
      mdcKeys.forEach { MDC.remove(it) }
      mdcKeys.clear()
    }
  }

  companion object {
    private const val SUCCESS = "success"
    private const val FAILURE = "failure"
    const val AZURE_KEY_VAULT_DEPENDENCY: String = "azure-key-vault"
    const val REDIS_DEPENDENCY: String = "eCommerce-redis"
    const val MONGO_DEPENDENCY: String = "eCommerce-mongodb"

    private val OBJECT_MAPPER: ObjectMapper =
      ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @JvmStatic fun loggerTracingUtils(): LogTracingUtils = LogTracingUtils()

    private fun serializeDetailsToMdcMap(details: Map<String, *>?): String {
      return details?.let {
        runCatching { OBJECT_MAPPER.writeValueAsString(it) }.getOrDefault("{}")
      }
        ?: "{}"
    }

    @JvmStatic
    fun enrichContextForEvent(
      tracingEntries: Map<AttributeKeys, String?>?,
      reactorContext: Context,
    ): Context {
      if (tracingEntries == null) return reactorContext
      return tracingEntries.entries.fold(reactorContext) { context, (key, value) ->
        context.put(key.key, value ?: key.defaultValue)
      }
    }
  }
}
