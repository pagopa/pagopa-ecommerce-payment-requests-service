package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnurlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.utils.CartRequestes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.net.URI

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class CartsServiceTests {

    @Autowired
    lateinit var cartService: CartService


    @Test
    fun `post cart succeeded with one payment notice`() {
        val request = CartRequestes.withOnePaymentNotice()
        val locationUrl = "http://checkout-url.it/77777777777302000100440009424"
        assertEquals(locationUrl, cartService.processCart(request))
    }

    @Test
    fun `post cart ko with multiple payment notices`() {
        val request = CartRequestes.withMultiplePaymentNotice()
        assertThrows<RestApiException> {
            cartService.processCart(request)
        }
    }

    @Test
    fun `get cart by id`() {
        val request = CartRequestDto(
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
        assertEquals(request, cartService.getCart("cartId"))
    }
}