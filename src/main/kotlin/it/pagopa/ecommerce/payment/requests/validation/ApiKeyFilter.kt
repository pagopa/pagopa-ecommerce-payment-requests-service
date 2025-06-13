package it.pagopa.ecommerce.payment.requests.validation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ApiKeyFilter(
    @Value("\${security.apiKey.primary}") private val primaryApiKey: String,
    @Value("\${security.apiKey.secondary}") private val secondaryApiKey: String,
) : WebFilter {
    private val pathsToCheck = listOf("/carts", "/payment-requests")

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.toString()
        if (pathsToCheck.any { path.startsWith(it) }) {
            val apiKey = exchange.request.headers.getFirst("X-Api-Key")
            if (apiKey.isNullOrBlank() || (apiKey != primaryApiKey && apiKey != secondaryApiKey)) {
                exchange.response.statusCode = org.springframework.http.HttpStatus.UNAUTHORIZED
                return exchange.response.setComplete()
            }
        }
        return chain.filter(exchange)
    }
}