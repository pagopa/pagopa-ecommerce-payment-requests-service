package it.pagopa.ecommerce.payment.requests.validation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ApiKeyFilter(
  @Value("\${security.apiKey.primary}") private val primaryApiKey: String,
  @Value("\${security.apiKey.secondary}") private val secondaryApiKey: String,
  @Value("\${security.apiKey.securedPaths}") private val securedPaths: List<String>,
) : WebFilter {
  private var logger: Logger = LoggerFactory.getLogger(this.javaClass)
  private val validApiKeys = setOf(primaryApiKey, secondaryApiKey)
  /*
   * @formatter:off
   *
   * Warning kotlin:S6508 - "Unit" should be used instead of "Void"
   * Suppressed because Spring WebFilter interface use Void as return type.
   *
   * @formatter:on
   */
  @SuppressWarnings("kotlin:S6508")
  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
    val path = exchange.request.path.toString()
    if (securedPaths.any { path.startsWith(it) }) {
      val apiKey = exchange.request.headers.getFirst("X-Api-Key")
      if (!isValidApiKey(apiKey)) {
        logger.error("Unauthorized request for path $path - Missing or invalid API key")
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
      }
    }
    return chain.filter(exchange)
  }

  private fun isValidApiKey(apiKey: String?): Boolean {
    return !apiKey.isNullOrBlank() && validApiKeys.contains(apiKey)
  }
}
