package it.pagopa.ecommerce.payment.requests.exceptions

import it.pagopa.generated.nodoperpsp.model.FaultBean
import java.util.regex.Pattern

class NodoErrorException(val faultCode: String) : RuntimeException() {

    constructor(faultBean: FaultBean) : this(getFaultCodeFromBean(faultBean))


    companion object {
        var faultCodePattern: Pattern = Pattern.compile("(PAA|PPT)_\\S+")
        fun getFaultCodeFromBean(faultBean: FaultBean): String {
            val description: String? = faultBean.description
            return if (description != null) {
                val matcher = faultCodePattern.matcher(description)
                if (matcher.find()) {
                    matcher.group()
                } else {
                    faultBean.faultCode
                }
            } else {
                faultBean.faultCode
            }
        }

    }
}