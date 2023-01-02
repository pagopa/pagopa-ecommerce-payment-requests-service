package it.pagopa.ecommerce.payment.requests.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class RptIdTests {

  companion object {
    const val VALID_FISCAL_CODE = "32009090901"
    const val VALID_NOTICE_CODE = "002016723749670035"
    const val VALID_RPT_ID = VALID_FISCAL_CODE + VALID_NOTICE_CODE
  }

  @Test
  fun `empty RptId is invalid`() {
    assertThrows<IllegalArgumentException> { RptId("") }
  }

  @Test
  fun `RptId too long is rejected`() {
    assertThrows<IllegalArgumentException> { RptId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa") }
  }

  @Test
  fun `valid RptId is ok`() {
    assertDoesNotThrow { RptId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa") }
  }

  @Test
  fun `should return rpt id`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals(VALID_RPT_ID, rptId.value)
  }

  @Test
  fun `should return fiscal code`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals(VALID_FISCAL_CODE, rptId.fiscalCode)
  }

  @Test
  fun `should return notice code`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals(VALID_NOTICE_CODE, rptId.noticeId)
  }

  @Test
  fun `should return aux digit`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals("0", rptId.auxDigit)
  }

  @Test
  fun `should return application code`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals("02", rptId.applicationCode)
  }

  @Test
  fun `should return IUV code`() {
    val rptId = RptId(VALID_RPT_ID)
    assertEquals("016723749670035", rptId.IUV)
  }

  @Test
  fun `should get same RPT id`() {
    val rptId1 = RptId(VALID_RPT_ID)
    val rptId2 = RptId(VALID_RPT_ID)
    assertTrue(rptId1 == rptId2)
    assertEquals(rptId1.hashCode(), rptId2.hashCode())
  }
}
