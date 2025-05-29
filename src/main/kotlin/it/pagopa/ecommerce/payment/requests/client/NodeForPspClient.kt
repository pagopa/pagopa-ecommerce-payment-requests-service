package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import jakarta.xml.bind.JAXBElement
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Component
class NodeForPspClient(
  @Value("\${nodo.nodeforpsp.uri}") private val nodoForPspUrl: String,
  @Autowired private val nodoWebClient: WebClient,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun verifyPaymentNotice(
    request: JAXBElement<VerifyPaymentNoticeReq>
  ): Mono<VerifyPaymentNoticeRes> =
    nodoWebClient
      .post()
      .uri("http://localhost:9999/wsdl")
      .header("Content-Type", MediaType.TEXT_XML_VALUE)
      .header("SOAPAction", "verifyPaymentNotice")
      .body(Mono.just(SoapEnvelope("", request)), SoapEnvelope::class.java)
      .retrieve()
      .onStatus(HttpStatusCode::isError) { clientResponse ->
        clientResponse.bodyToMono(String::class.java).flatMap { errorResponseBody: String ->
          Mono.error(ResponseStatusException(clientResponse.statusCode(), errorResponseBody))
        }
      }
      .bodyToMono(VerifyPaymentNoticeRes::class.java)
      .doOnSuccess {
        logger.debug("Payment activated with payment token [{}]", request.value.qrCode.noticeNumber)
      }
      .doOnError(ResponseStatusException::class.java) { logger.error("Response status error", it) }
      .doOnError(Exception::class.java) { logger.error("Generic error", it) }
}
