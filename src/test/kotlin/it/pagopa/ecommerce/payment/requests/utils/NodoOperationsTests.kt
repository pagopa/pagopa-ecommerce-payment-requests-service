package it.pagopa.ecommerce.payment.requests.utils

import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NodoOperationsTests {

  @Test
  fun `should return euro cents amount`() {
    val nodoOperations = NodoOperations()
    val amount = BigDecimal.valueOf(100.50)
    val euroCentAmount = amount.multiply(BigDecimal(100)).toLong()
    assertEquals(euroCentAmount, nodoOperations.getEuroCentsFromNodoAmount(amount))
  }
}
