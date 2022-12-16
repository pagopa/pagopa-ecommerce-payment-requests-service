package it.pagopa.ecommerce.payment.requests.client;

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Component
public class NodoPerPmClient(
    @Value("\${nodo.nodoperpm.uri}") private val nodoPerPmUrl: String,
    @Autowired private val nodoPerPmClient: WebClient
    ) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun checkPosition(request: CheckPositionDto): Mono<CheckPositionResponseDto> {
        return nodoPerPmClient.post()
            .uri(nodoPerPmUrl)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(request), CheckPositionDto::class.java)
            .retrieve()
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).flatMap { errorResponseBody: String ->
                    Mono.error(
                        ResponseStatusException(HttpStatus.BAD_GATEWAY, errorResponseBody)
                    )
                }
            }.bodyToMono(CheckPositionResponseDto::class.java)
            .doOnSuccess {
                logger.debug(
                    "Check position called successfully with list [{}]", request.positionslist.toString()
                )}
            .doOnError(ResponseStatusException::class.java) {
                logger.error("Response status error", it)
            }.doOnError(Exception::class.java) { logger.error("Generic error", it) }
    }
}
