package it.pagopa.ecommerce.payment.requests.tests.utils

import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto

object PaymentRequests {

    fun validResponse(rptId: String) = PaymentRequestsGetResponseDto(
        amount = 12000,
        paymentContextCode = "88112be9dda8477ea6f55b537ae2f3cf",
        rptId = rptId,
        paFiscalCode = "77777777777",
        paName = "MockEC",
        description = "pagamento di test",
        dueDate = "2022-10-24"
    )
}