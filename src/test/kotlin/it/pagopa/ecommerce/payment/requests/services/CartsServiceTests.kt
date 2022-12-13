package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.CartNotFoundException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.repositories.CartInfo
import it.pagopa.ecommerce.payment.requests.repositories.CartInfoRepository
import it.pagopa.ecommerce.payment.requests.repositories.PaymentInfo
import it.pagopa.ecommerce.payment.requests.repositories.ReturnUrls
import it.pagopa.ecommerce.payment.requests.tests.utils.CartRequests
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.*
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class CartsServiceTests {
    companion object {
        const val TEST_CHECKOUT_URL: String = "https://test-checkout.url";
    }

    private val cartInfoRepository: CartInfoRepository = mock()
    private val nodoPerPmClient: NodoPerPmClient = mock()
    private val cartService: CartService = CartService(TEST_CHECKOUT_URL, cartInfoRepository, nodoPerPmClient)


    @Test
    fun `post cart succeeded with one payment notice`() = runTest {
        val cartId = UUID.randomUUID()

        Mockito.mockStatic(UUID::class.java).use { uuidMock ->
            uuidMock.`when`<UUID>(UUID::randomUUID).thenReturn(cartId)

            val request = CartRequests.withOnePaymentNotice()
            val locationUrl = "${TEST_CHECKOUT_URL}/c/${cartId}"
            assertEquals(locationUrl, cartService.processCart(request))
            verify(cartInfoRepository, times(1)).save(any())
        }
    }

    /*
    @Test
    fun `post cart ko with multiple payment notices`() = runTest {
        val request = CartRequests.withMultiplePaymentNotice()
        assertThrows<RestApiException> {
            cartService.processCart(request)
        }
    }
     */

    @Test
    fun `get cart by id`() {
        val cartId = UUID.randomUUID()

        Mockito.mockStatic(UUID::class.java).use { uuidMock ->
            uuidMock.`when`<UUID> { UUID.fromString(cartId.toString()) }.thenReturn(cartId)

            val request = CartRequests.withOnePaymentNotice()

            given(cartInfoRepository.findById(cartId)).willReturn(request.let { req ->
                val cartInfo = CartInfo(
                    cartId,
                    req.paymentNotices.map {
                        PaymentInfo(
                            RptId(it.fiscalCode + it.noticeNumber),
                            it.description,
                            it.amount,
                            it.companyName
                        )
                    },
                    req.returnUrls.let {
                        ReturnUrls(
                            returnSuccessUrl = it.returnOkUrl.toString(),
                            returnErrorUrl = it.returnErrorUrl.toString(),
                            returnCancelUrl = it.returnCancelUrl.toString()
                        )
                    },
                    req.emailNotice
                )

                Optional.of(cartInfo)
            })

            assertEquals(request, cartService.getCart(cartId))
        }
    }

    @Test
    fun `non-existing id throws CartNotFoundException`() {
        val cartId = UUID.randomUUID()

        given(cartInfoRepository.findById(cartId)).willReturn(Optional.empty())

        assertThrows<CartNotFoundException> {
            cartService.getCart(cartId)
        }
    }
}