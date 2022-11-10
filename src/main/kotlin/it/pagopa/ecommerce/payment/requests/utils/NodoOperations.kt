package it.pagopa.ecommerce.payment.requests.utils

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NodoOperations {

    fun getEuroCentsFromNodoAmount(amountFromNodo: BigDecimal): Int =
        amountFromNodo.multiply(BigDecimal.valueOf(100)).toInt()
}