package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.CartRequestReturnurlsDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentNoticeDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfoRepository
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.net.URI
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class CartsServiceTests {
    companion object {
        const val TEST_CHECKOUT_URL: String = "https://test-checkout.url";
    }

    private val cartInfoRepository: CartInfoRepository = mock()

    private val cartService: CartService = CartService(TEST_CHECKOUT_URL, cartInfoRepository)


    @Test
    fun `post cart succeeded with one payment notice`() = runTest {
        val cartId = UUID.randomUUID()

        Mockito.mockStatic(UUID::class.java).use { uuidMock ->
            uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)

            val request = CartRequests.withOnePaymentNotice()
            val locationUrl = "${TEST_CHECKOUT_URL}/carts/${cartId}"

            assertEquals(locationUrl, cartService.processCart(request))

            verify(cartInfoRepository, times(1)).save(any())
        }
    }

    @Test
    fun `post cart ko with multiple payment notices`() = runTest {
        val request = CartRequests.withMultiplePaymentNotice()
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