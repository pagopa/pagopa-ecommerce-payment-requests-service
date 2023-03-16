package it.pagopa.ecommerce.payment.requests.tests.utils

import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto

object PaymentRequests {

  fun validResponse(rptId: String) =
    PaymentRequestsGetResponseDto(
      amount = 12000,
      rptId = rptId,
      paFiscalCode = "77777777777",
      paName = "MockEC",
      description = "pagamento di test",
      dueDate = "2022-10-24")
}
