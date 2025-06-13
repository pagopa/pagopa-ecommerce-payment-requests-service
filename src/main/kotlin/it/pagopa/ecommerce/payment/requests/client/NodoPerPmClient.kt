package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseErrorDto
import it.pagopa.ecommerce.payment.requests.exceptions.CheckPositionErrorException
import java.util.function.Predicate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
public class NodoPerPmClient(
  @Value("\${nodo.nodoperpm.uri}") private val nodoPerPmUrl: String,
  @Autowired private val nodoPerPmClient: WebClient,
  @Value("\${nodo.nodeforecommerce.apikey}") private val nodeForEcommerceApiKey: String
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun checkPosition(request: CheckPositionDto): Mono<CheckPositionResponseDto> {
    return nodoPerPmClient
      .post()
      .uri(nodoPerPmUrl)
      .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
      .header("ocp-apim-subscription-key", nodeForEcommerceApiKey)
      .body(Mono.just(request), CheckPositionDto::class.java)
      .retrieve()
      .onStatus(Predicate.isEqual(HttpStatus.BAD_REQUEST)) { clientResponse ->
        clientResponse
          .bodyToMono(CheckPositionResponseErrorDto::class.java)
          .onErrorMap { CheckPositionErrorException(clientResponse.statusCode()) }
          .flatMap { Mono.error(CheckPositionErrorException(HttpStatus.UNPROCESSABLE_ENTITY)) }
      }
      .onStatus(HttpStatusCode::isError) { clientResponse ->
        Mono.error(CheckPositionErrorException(clientResponse.statusCode()))
      }
      .bodyToMono(CheckPositionResponseDto::class.java)
      .doOnSuccess {
        logger.debug(
          "Check position called successfully with list [{}]", request.positionslist.toString())
      }
  }
}
