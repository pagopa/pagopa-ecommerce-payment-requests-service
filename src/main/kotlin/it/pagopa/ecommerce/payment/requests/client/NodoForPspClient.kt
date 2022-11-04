package it.pagopa.ecommerce.payment.requests.client

import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import it.pagopa.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.generated.transactions.model.VerifyPaymentNoticeRes
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import javax.xml.bind.JAXBElement

@Component
class NodoForPspClient(
    @Value("\${nodo.nodeforpsp.uri}") val nodoForPspUrl: String,
    @Autowired val nodoWebClient: WebClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun verifyPaymentNotice(request: JAXBElement<VerifyPaymentNoticeReq>): VerifyPaymentNoticeRes =
        nodoWebClient.post()
            .uri(nodoForPspUrl)
            .header("Content-Type", MediaType.TEXT_XML_VALUE)
            .header("SOAPAction", "verifyPaymentNotice")
            .body(SoapEnvelope("", request), SoapEnvelope::class.java)
            .retrieve()
            .onStatus(HttpStatus::isError) { clientResponse ->
                clientResponse.bodyToMono(String::class.java).flatMap { errorResponseBody: String ->
                    Mono.error(
                        ResponseStatusException(clientResponse.statusCode(), errorResponseBody)
                    )
                }
            }.bodyToMono(VerifyPaymentNoticeRes::class.java)
            .doOnSuccess() {
                logger.debug(
                    "Payment activated with payment token [{}]", request.value.qrCode.noticeNumber
                )
            }.doOnError(ResponseStatusException::class.java) {
                logger.error("Response status error", it)
            }.doOnError(Exception::class.java) { logger.error("Generic error", it) }.awaitSingle()

}