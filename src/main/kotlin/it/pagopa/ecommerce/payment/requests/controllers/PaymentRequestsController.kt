package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.PaymentRequestsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.payment.requests.services.PaymentRequestsService
import it.pagopa.ecommerce.payment.requests.warmup.annotations.WarmupFunction
import it.pagopa.ecommerce.payment.requests.warmup.utils.WarmupRequests
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
class PaymentRequestsController(
    private val restTemplate: RestTemplate = RestTemplate()
) : PaymentRequestsApi {

    @Autowired
    private lateinit var paymentRequestsService: PaymentRequestsService

    override suspend fun getPaymentRequestInfo(rptId: String): ResponseEntity<PaymentRequestsGetResponseDto> {
        return ResponseEntity.ok(
            paymentRequestsService.getPaymentRequestInfo(rptId)
        )
    }

    /**
     * Controller warm up function, used to send a POST carts request
     */
    @WarmupFunction
    fun warmupGetPaymentRequest() {
        restTemplate.getForEntity(
            "http://localhost:8080/payment-requests/{rpt_id}",
            PaymentRequestsGetResponseDto::class.java,
            mapOf<String, String>("rpt_id" to WarmupRequests.getPaymentRequest())
        )
    }
}
