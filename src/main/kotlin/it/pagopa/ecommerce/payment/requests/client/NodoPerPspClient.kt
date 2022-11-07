package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import it.pagopa.generated.nodoperpsp.model.NodoVerificaRPT
import it.pagopa.generated.nodoperpsp.model.NodoVerificaRPTRisposta
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import javax.xml.bind.JAXBElement

@Component
class NodoPerPspClient(
    @Value("\${nodo.nodoperpsp.uri}") val nodoPerPspUrl: String,
    @Autowired val nodoWebClient: WebClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun verificaRpt(request: JAXBElement<NodoVerificaRPT>) =
        nodoWebClient.post()
            .uri(nodoPerPspUrl)
            .header("Content-Type", MediaType.TEXT_XML_VALUE)
            .header("SOAPAction", "nodoVerificaRPT")
            .body(SoapEnvelope("", request), SoapEnvelope::class.java)
            .retrieve()
            .onStatus(
                HttpStatus::isError
            ) { clientResponse: ClientResponse ->
                clientResponse.bodyToMono(
                    String::class.java
                ).flatMap { errorResponseBody: String ->
                    Mono.error(
                        ResponseStatusException(clientResponse.statusCode(), errorResponseBody)
                    )
                }
            }.bodyToMono(NodoVerificaRPTRisposta::class.java)
            .doOnSuccess() { logger.debug("Payment info for {}", request.value.codiceIdRPT) }
            .doOnError(ResponseStatusException::class.java) { logger.error("ResponseStatus Error: ", it) }
            .doOnError(Exception::class.java) { logger.error("Generic exception: ", it) }


}