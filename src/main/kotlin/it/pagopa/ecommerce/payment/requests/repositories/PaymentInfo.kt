package it.pagopa.ecommerce.payment.requests.repositories

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.data.annotation.PersistenceCreator

data class PaymentInfo
@PersistenceCreator
constructor(val rptId: RptId, val description: String, val amount: Int, val companyName: String)
