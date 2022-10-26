package it.pagopa.ecommerce.payment.requests.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.payment.requests.services.CartService
import it.pagopa.ecommerce.payment.requests.validation.BeanValidationConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(BeanValidationConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PaymentRequestsControllerTests {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    lateinit var cartService: CartService

    @Test
    fun `get payment request info`() {
        val objectMapper = ObjectMapper()
        val response = PaymentRequestsGetResponseDto(
            amount = 12000,
            paymentContextCode = "88112be9dda8477ea6f55b537ae2f3cf",
            rptId = "77777777777302000100000009424",
            paFiscalCode = "77777777777",
            paName = "MockEC",
            description = "pagamento di test",
            dueDate = "2022-10-24"
        )
        val parameters = mapOf("rpt_id" to "1234")
        webClient.get()
            .uri("/payment-requests/{rpt_id}", parameters)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(objectMapper.writeValueAsString(response))
    }
}