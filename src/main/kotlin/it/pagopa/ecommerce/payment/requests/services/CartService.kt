package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnUrlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.CartInfoRepository
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.net.URI
import java.util.*

@Service
class CartService(
    @Value("\${checkout.url}") private val checkoutUrl: String,
    @Autowired private val cartInfoRepository: CartInfoRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /*
     * Logger instance
     */
    var logger: Logger = LoggerFactory.getLogger(this.javaClass)

    companion object CartServiceConstants {
        /*
         * Carts redirect URL format.
         * The carts redirect url is composed as follow
         * {host}/c/{cartId}
         */
        const val CARTS_REDIRECT_URL_FORMAT: String = "%s/c/%s"

        const val MAX_ALLOWED_PAYMENT_NOTICES: Int = 1
    }

    /*
     * Process input cartRequestDto:
     * - 1 payment notice is present -> redirect response is given to the checkout location
     * - 2 or more payment notices are present -> error response
     */
    suspend fun processCart(cartRequestDto: CartRequestDto): String {
        val paymentsNotices = cartRequestDto.paymentNotices
        val receivedNotices = paymentsNotices.size
        logger.info("Received [$receivedNotices] payment notices")

        return if (receivedNotices == MAX_ALLOWED_PAYMENT_NOTICES) {
            val paymentInfos = paymentsNotices.map {
                PaymentInfo(RptId(it.fiscalCode + it.noticeNumber), it.description, it.amount, it.companyName)
            }

            val cart = CartInfo(
                UUID.randomUUID(),
                paymentInfos,
                ReturnUrls(
                    returnSuccessUrl = cartRequestDto.returnUrls.returnOkUrl.toString(),
                    returnErrorUrl = cartRequestDto.returnUrls.returnErrorUrl.toString(),
                    returnCancelUrl = cartRequestDto.returnUrls.returnCancelUrl.toString()
                ),
                cartRequestDto.emailNotice
            )

            logger.info("Saving cart ${cart.cartId} for payments $paymentInfos")

            withContext(defaultDispatcher) {
                cartInfoRepository.save(cart)
            }

            CARTS_REDIRECT_URL_FORMAT.format(
                checkoutUrl,
                cart.cartId,

                )
        } else {
            logger.error("Too many payment notices, expected only one")
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
    fun getCart(cartId: UUID): CartRequestDto {
        val cart = cartInfoRepository.findByIdOrNull(cartId) ?: throw CartNotFoundException(cartId.toString())

        return CartRequestDto(
            paymentNotices = cart.payments.map {
                PaymentNoticeDto(it.rptId.noticeId, it.rptId.fiscalCode, it.amount, it.companyName, it.description)
            },
            returnUrls = cart.returnUrls.let {
                CartRequestReturnUrlsDto(
                    returnOkUrl = URI(it.returnSuccessUrl),
                    returnCancelUrl = URI(it.returnCancelUrl),
                    returnErrorUrl = URI(it.returnErrorUrl)
                )
            },
            emailNotice = cart.email
        )
    }
}
