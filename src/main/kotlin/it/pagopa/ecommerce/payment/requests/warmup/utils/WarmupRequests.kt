package it.pagopa.ecommerce.payment.requests.warmup.utils

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import java.net.URI

object WarmupRequests {

    fun postCartsReq() = CartRequestDto(
        paymentNotices = listOf(
            PaymentNoticeDto(
                noticeNumber = "000000000000000000",
                fiscalCode = "00000000000",
                amount = 1,
                companyName = "test-warm-up-req",
                description = "test-warm-up-req"
            )
        ),
        returnUrls = CartRequestReturnUrlsDto(
            returnOkUrl = URI("www.warmup-req-ok.it"),
            returnCancelUrl = URI("www.warmup-req-cancel.it"),
            returnErrorUrl = URI("www.warmup-req-error.it"),
        ),
        emailNotice = "my_email@mail.it"
    )

    fun getPaymentRequest(): String = "00000000000000000000000000000"
}