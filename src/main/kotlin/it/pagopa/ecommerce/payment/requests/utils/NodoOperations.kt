package it.pagopa.ecommerce.payment.requests.utils

import java.math.BigDecimal
import org.springframework.stereotype.Component

@Component
class NodoOperations {

  fun getEuroCentsFromNodoAmount(amountFromNodo: BigDecimal): Int =
    amountFromNodo.multiply(BigDecimal.valueOf(100)).toInt()
}
