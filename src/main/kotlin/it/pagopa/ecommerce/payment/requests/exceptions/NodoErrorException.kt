package it.pagopa.ecommerce.payment.requests.exceptions

import it.pagopa.ecommerce.generated.transactions.model.CtFaultBean
import java.util.regex.Pattern

class NodoErrorException(val faultCode: String) :
  RuntimeException("Exception communication with nodo. Fault code: [${faultCode}]") {

  constructor(
    faultBean: CtFaultBean?
  ) : this(getFaultCodeFromBean(faultBean?.faultCode, faultBean?.description))

  companion object {
    var faultCodePattern: Pattern = Pattern.compile("(PAA|PPT)_\\S+")
    fun getFaultCodeFromBean(faultCode: String?, description: String?): String {
      val extractedFaultCode =
        if (description != null) {
          val matcher = faultCodePattern.matcher(description)
          if (matcher.find()) {
            matcher.group()
          } else {
            faultCode
          }
        } else {
          faultCode
        }
      return extractedFaultCode ?: "Unreadable fault code"
    }
  }
}
