package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnurlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.net.URI

@Service
class CartService {

    /*
     * Logger instance
     */
    var logger: Logger = LoggerFactory.getLogger(this.javaClass)

    object CartServiceConstants {
        /*
         * Carts redirect URL format.
         * The carts redirect url is composed as follow
         * {host}/{fiscalCode}{noticeNumber}
         */
        const val CARTS_REDIRECT_URL_FORMAT: String = "%s/%s%s"
    }

    @Value("\${checkout.url}")
    lateinit var checkoutUrl: String

    /*
     * Process input cartRequestDto:
     * - 1 payment notice is present -> redirect response is given to the checkout location
     * - 2 or more payment notices are present -> error response
     */
    fun processCart(cartRequestDto: CartRequestDto): String {
        val paymentsNotices = cartRequestDto.paymentNotices
        val receivedNotices = paymentsNotices.size
        logger.info("Received [$receivedNotices] payment notices")
        return if (receivedNotices == 1) {
            val paymentNotice = paymentsNotices[0]
            CartServiceConstants.CARTS_REDIRECT_URL_FORMAT.format(
                checkoutUrl,
                paymentNotice.fiscalCode,
                paymentNotice.noticeNumber
            )
        } else {
            logger.error("Too many payment notices, expected only one")
            //TODO capire la risposta di errore da mettere qui
            throw RestApiException(
                httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
                title = "Multiple payment notices not processable",
                description = "Too many payment notices, expected max one"
            )
        }
    }

    /*
     * Fetch the cart with the input cart id
     */
    fun getCart(cartId: String): CartRequestDto {
        return CartRequestDto(
            paymentNotices = listOf(
                PaymentNoticeDto(
                    noticeNumber = "302000100440009424",
                    fiscalCode = "77777777777",
                    amount = 10000
                )
            ),
            returnurls = CartRequestReturnurlsDto(
                retunErrorUrl = URI.create("https://returnErrorUrl"),
                returnOkUrl = URI.create("https://returnOkUrl"),
                returnCancelUrl = URI.create("https://returnCancelUrl")
            ),
            emailNotice = "test@test.it"
        )
    }
}