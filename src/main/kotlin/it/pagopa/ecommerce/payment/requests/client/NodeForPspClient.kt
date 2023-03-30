package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import javax.xml.bind.JAXBElement
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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
      .uri(nodoForPspUrl)
      .header("Content-Type", MediaType.TEXT_XML_VALUE)
      .header("SOAPAction", "verifyPaymentNotice")
      .body(Mono.just(SoapEnvelope("", request)), SoapEnvelope::class.java)
      .retrieve()
      .onStatus(HttpStatus::isError) { clientResponse ->
        Mono.error(
          RestApiException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Error while verify payment notices",
            "Error performing verifyPaymentNotice.Received HTTP code ${clientResponse.statusCode()}",
          ))
      }
      .bodyToMono(VerifyPaymentNoticeRes::class.java)
      .doOnSuccess {
        logger.debug("Payment activated with payment token [{}]", request.value.qrCode.noticeNumber)
      }
      .doOnError(Exception::class.java) { logger.error("Generic error", it) }
}
