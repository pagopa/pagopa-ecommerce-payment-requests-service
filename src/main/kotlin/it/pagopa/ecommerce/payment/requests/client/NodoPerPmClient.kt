package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
public class NodoPerPmClient(
  @Value("\${nodo.nodoperpm.uri}") private val nodoPerPmUrl: String,
  @Autowired private val nodoPerPmClient: WebClient
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun checkPosition(request: CheckPositionDto): Mono<CheckPositionResponseDto> {
    return nodoPerPmClient
      .post()
      .uri(nodoPerPmUrl)
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .body(Mono.just(request), CheckPositionDto::class.java)
      .retrieve()
      .onStatus(HttpStatus::isError) { clientResponse ->
        Mono.error(
          RestApiException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Error while checking payment notices",
            "Error performing checkPosition. Received HTTP code ${clientResponse.statusCode()}"))
      }
      .bodyToMono(CheckPositionResponseDto::class.java)
      .doOnSuccess {
        logger.debug(
          "Check position called successfully with list [{}]", request.positionslist.toString())
      }
      .doOnError(Exception::class.java) { logger.error("Generic error", it) }
  }
}
