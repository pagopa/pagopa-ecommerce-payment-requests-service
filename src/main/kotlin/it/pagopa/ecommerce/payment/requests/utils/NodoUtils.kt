package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.generated.nodoperpsp.model.NodoTipoCodiceIdRPT
import it.pagopa.ecommerce.generated.nodoperpsp.model.QrCode
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class NodoUtils(
  @Autowired
  private val objectFactoryNodoPerPsp: it.pagopa.ecommerce.generated.nodoperpsp.model.ObjectFactory
) {
  fun getCodiceIdRpt(rptId: RptId): NodoTipoCodiceIdRPT {
    val nodoTipoCodiceIdRPT = objectFactoryNodoPerPsp.createNodoTipoCodiceIdRPT()
    val qrCode = QrCode()
    qrCode.cf = rptId.fiscalCode
    qrCode.auxDigit = rptId.auxDigit
    qrCode.codIUV = rptId.IUV
    if (auxDigitZero(rptId.auxDigit)) {
      qrCode.codStazPA = rptId.applicationCode
    }
    nodoTipoCodiceIdRPT.qrCode = qrCode
    return nodoTipoCodiceIdRPT
  }

  private fun auxDigitZero(auxDigit: String): Boolean {
    return "0" == auxDigit
  }
}
