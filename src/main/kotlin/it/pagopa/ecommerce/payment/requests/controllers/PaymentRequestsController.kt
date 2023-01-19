package it.pagopa.ecommerce.payment.requests.controllers

import PaymentRequestsService
import it.pagopa.ecommerce.generated.payment.requests.server.api.PaymentRequestsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import it.pagopa.ecommerce.payment.requests.warmup.exceptions.WarmUpException
import it.pagopa.ecommerce.payment.requests.warmup.utils.WarmupRequests
import java.time.Duration
import java.time.temporal.ChronoUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
class PaymentRequestsController(private val webClient: WebClient = WebClient.create()) :
  PaymentRequestsApi {

  @Autowired private lateinit var paymentRequestsService: PaymentRequestsService

  override suspend fun getPaymentRequestInfo(
    rptId: String
  ): ResponseEntity<PaymentRequestsGetResponseDto> {
    return ResponseEntity.ok(paymentRequestsService.getPaymentRequestInfo(rptId))
  }

  /** Controller warm up function, used to send a GET payment-request */
  @WarmupFunction
  fun warmupGetPaymentRequest() {
    webClient
      .get()
      .uri(
        "http://localhost:8080/payment-requests/{rpt_id}",
        mapOf("rpt_id" to WarmupRequests.getPaymentRequest()))
      .retrieve()
      .onStatus(HttpStatus::isError) {
        Mono.error(WarmUpException("PaymentRequestsController", "warmupGetPaymentRequest"))
      }
      .toBodilessEntity()
      .block(Duration.of(10, ChronoUnit.SECONDS))
  }
}
