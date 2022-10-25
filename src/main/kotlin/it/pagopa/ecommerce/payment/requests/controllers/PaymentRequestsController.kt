package it.pagopa.ecommerce.payment.requests.controllers

import org.springframework.web.bind.annotation.RestController
import it.pagopa.ecommerce.generated.payment.requests.server.api.PaymentRequestsApi
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import org.springframework.http.ResponseEntity

@RestController
class PaymentRequestsController(

): PaymentRequestsApi{

    override suspend fun getPaymentRequestInfo(rptId: String): ResponseEntity<PaymentRequestsGetResponseDto> {
        return ResponseEntity.ok(
            PaymentRequestsGetResponseDto(
                amount = 12000,
                paymentContextCode = "88112be9dda8477ea6f55b537ae2f3cf",
                rptId = "77777777777302000100000009424",
                paFiscalCode = "77777777777",
                paName = "MockEC",
                description = "pagamento di test",
                dueDate= "2022-10-24"
            )
        )
    }
}
