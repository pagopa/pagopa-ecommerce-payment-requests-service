package it.pagopa.ecommerce.payment.requests.controllers

import it.pagopa.ecommerce.generated.payment.requests.server.api.PaymentRequestsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.payment.requests.services.PaymentRequestsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentRequestsController(
    @Autowired
    val paymentRequestsService: PaymentRequestsService
) : PaymentRequestsApi {

    override suspend fun getPaymentRequestInfo(rptId: String): ResponseEntity<PaymentRequestsGetResponseDto> {
        return ResponseEntity.ok(
            paymentRequestsService.getPaymentRequestInfo(rptId)
        )
    }
}
