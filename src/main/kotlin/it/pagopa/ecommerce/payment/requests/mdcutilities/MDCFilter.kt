package it.pagopa.ecommerce.payment.requests.mdcutilities

import io.micrometer.context.ContextRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.util.context.Context

@Component
class MDCFilter : WebFilter {

  companion object {
    val contextBound =
      setOf(
        LogTracingUtils.AttributeKeys.CTX_RPT_IDS.key,
        LogTracingUtils.AttributeKeys.EVENT_ACTION.key,
        LogTracingUtils.AttributeKeys.CTX_AUTHORIZATION_REQUEST_ID.key,
      )
  }

  @PostConstruct
  fun initMdcMicrometerRegistry() {
    Hooks.enableAutomaticContextPropagation()
    LogTracingUtils.AttributeKeys.entries
      .filter { contextBound.contains(it.key) }
      .forEach { entry ->
        ContextRegistry.getInstance()
          .registerThreadLocalAccessor(
            entry.key,
            { MDC.get(entry.key) },
            { value -> MDC.put(entry.key, value) },
            { MDC.remove(entry.key) })
      }
  }

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
    val method = exchange.request.method
    val path = exchange.request.path

    val mdcContext = Context.of(LogTracingUtils.AttributeKeys.EVENT_ACTION.key, "$method $path")

    return chain.filter(exchange).contextWrite(mdcContext)
  }
}
