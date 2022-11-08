package it.pagopa.ecommerce.payment.requests.tests.utils

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnurlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import java.net.URI

object CartRequests {

    fun withOnePaymentNotice(): CartRequestDto {
        return CartRequestDto(
            paymentNotices = listOf(
                PaymentNoticeDto(
                    noticeNumber = "302000100440009424",
                    fiscalCode = "77777777777",
                    amount = 10000
                )
            ),
            returnurls = CartRequestReturnurlsDto(
                returnOkUrl = URI("www.comune.di.prova.it/pagopa/success.html"),
                returnCancelUrl = URI("www.comune.di.prova.it/pagopa/cancel.html"),
                returnErrorUrl = URI("www.comune.di.prova.it/pagopa/error.html"),
            ),
            emailNotice = "my_email@mail.it"
        )
    }

    fun withMultiplePaymentNotice(): CartRequestDto {
        return CartRequestDto(
            paymentNotices = listOf(
                PaymentNoticeDto(
                    noticeNumber = "302000100440009420",
                    fiscalCode = "77777777777",
                    amount = 10000
                ),
                PaymentNoticeDto(
                    noticeNumber = "302000100440009421",
                    fiscalCode = "77777777777",
                    amount = 10000
                )
            ),
            returnurls = CartRequestReturnurlsDto(
                returnOkUrl = URI("www.comune.di.prova.it/pagopa/success.html"),
                returnCancelUrl = URI("www.comune.di.prova.it/pagopa/cancel.html"),
                returnErrorUrl = URI("www.comune.di.prova.it/pagopa/error.html"),
            )
        )
    }

    fun invalidRequest(): CartRequestDto {
        return CartRequestDto(
            paymentNotices = listOf(
                PaymentNoticeDto(
                    noticeNumber = "1",
                    fiscalCode = "1",
                    amount = 10000
                )
            ),
            returnurls = CartRequestReturnurlsDto(
                returnOkUrl = URI("www.comune.di.prova.it/pagopa/success.html"),
                returnCancelUrl = URI("www.comune.di.prova.it/pagopa/cancel.html"),
                returnErrorUrl = URI("www.comune.di.prova.it/pagopa/error.html"),
            )
        )
    }
}