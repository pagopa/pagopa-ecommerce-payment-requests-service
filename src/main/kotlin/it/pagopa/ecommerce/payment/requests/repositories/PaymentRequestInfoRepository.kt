package it.pagopa.ecommerce.payment.requests.repositories

import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.data.repository.CrudRepository

interface PaymentRequestInfoRepository : CrudRepository<PaymentRequestInfo, RptId>
