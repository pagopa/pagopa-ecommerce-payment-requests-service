package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.generated.nodoperpsp.model.ObjectFactory
import it.pagopa.ecommerce.payment.requests.domain.RptId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NodoUtilsTests {

  lateinit var nodoUtilities: NodoUtils

  @BeforeEach
  fun init() {
    nodoUtilities = NodoUtils(ObjectFactory())
  }

  @Test
  fun `should get nodo tipo codice id RPT 0`() {
    val rptId = RptId("77777777777011222222222222222")
    val nodoTipoCodiceIdRPT = nodoUtilities.getCodiceIdRpt(rptId)
    assertEquals("11", nodoTipoCodiceIdRPT.qrCode.codStazPA)
    assertEquals("0", nodoTipoCodiceIdRPT.qrCode.auxDigit)
    assertEquals("77777777777", nodoTipoCodiceIdRPT.qrCode.cf)
    assertEquals("222222222222222", nodoTipoCodiceIdRPT.qrCode.codIUV)
  }

  fun `should get nodo tipo codice id RPT aux`(auxDigit: String) {
    val rptId = RptId("77777777777" + auxDigit + "44444444444444444")
    val nodoTipoCodiceIdRPT = nodoUtilities.getCodiceIdRpt(rptId)
    assertNull(nodoTipoCodiceIdRPT.qrCode.codStazPA)
    assertEquals(auxDigit, nodoTipoCodiceIdRPT.qrCode.auxDigit)
    assertEquals("77777777777", nodoTipoCodiceIdRPT.qrCode.cf)
    assertEquals("44444444444444444", nodoTipoCodiceIdRPT.qrCode.codIUV)
  }

  @Test
  fun `should get nodo tipo codice id RPT greater than 0`() {
    `should get nodo tipo codice id RPT aux`("1")
    `should get nodo tipo codice id RPT aux`("2")
    `should get nodo tipo codice id RPT aux`("3")
  }
}
