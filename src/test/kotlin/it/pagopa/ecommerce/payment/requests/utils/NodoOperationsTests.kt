package it.pagopa.ecommerce.payment.requests.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class NodoOperationsTests {

    @Test
    fun `should return euro cents amount`() {
        val nodoOperations = NodoOperations()
        val amount = BigDecimal.valueOf(100.50)
        val euroCentAmount = amount.multiply(BigDecimal(100)).toInt()
        assertEquals(euroCentAmount, nodoOperations.getEuroCentsFromNodoAmount(amount))
    }
}