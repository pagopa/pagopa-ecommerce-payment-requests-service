package it.pagopa.ecommerce.payment.requests.repositories

import it.pagopa.ecommerce.commons.domain.v1.RptId
import org.springframework.data.annotation.PersistenceCreator

data class PaymentInfo
@PersistenceCreator
constructor(val rptId: RptId, val description: String, val amount: Int, val companyName: String)
