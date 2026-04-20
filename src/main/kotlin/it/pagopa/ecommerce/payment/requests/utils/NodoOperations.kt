package it.pagopa.ecommerce.payment.requests.utils

import java.math.BigDecimal
import org.springframework.stereotype.Component

@Component
class NodoOperations {

  fun getEuroCentsFromNodoAmount(amountFromNodo: BigDecimal): Long =
    amountFromNodo.multiply(BigDecimal.valueOf(100)).toLong()
}
