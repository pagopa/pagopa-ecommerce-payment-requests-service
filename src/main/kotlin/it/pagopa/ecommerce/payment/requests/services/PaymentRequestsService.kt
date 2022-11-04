package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfoRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PaymentRequestsService(@Autowired private val paymentRequestInfoRepository: PaymentRequestInfoRepository) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE = "PPT_MULTI_BENEFICIARIO"
    }

    suspend fun getPaymentRequestInfo(rptId: RptId): PaymentRequestsGetResponseDto {


        return PaymentRequestsGetResponseDto(1, "", "", "", "", "", "")
    }
}