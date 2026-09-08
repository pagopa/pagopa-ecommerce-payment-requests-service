package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils
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
  @Value("\${nodo.nodeforpsp.apikey}") private val nodoPerPspApiKey: String
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
      .header("ocp-apim-subscription-key", nodoPerPspApiKey)
      .body(Mono.just(SoapEnvelope("", request)), SoapEnvelope::class.java)
      .retrieve()
      .onStatus(HttpStatusCode::isError) { clientResponse ->
        clientResponse.bodyToMono(String::class.java).flatMap { errorResponseBody: String ->
          Mono.error(ResponseStatusException(clientResponse.statusCode(), errorResponseBody))
        }
      }
      .bodyToMono(VerifyPaymentNoticeRes::class.java)
      .doOnSuccess {
        LogTracingUtils.loggerTracingUtils()
          .attributes(mapOf(AttributeKeys.CTX_RPT_IDS to request.value.qrCode.fiscalCode + request.value.qrCode.noticeNumber))
          .logDebug(logger, "Node verifyPaymentNotice OK")
      .doOnError(ResponseStatusException::class.java) {
        LogTracingUtils.loggerTracingUtils().failure().logError(logger, it, "Response status error")
      }
      .doOnError(Exception::class.java) {
        LogTracingUtils.loggerTracingUtils().failure().logError(logger, it, "Generic error")
      }
}
