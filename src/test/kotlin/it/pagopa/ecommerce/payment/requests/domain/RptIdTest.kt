package it.pagopa.ecommerce.payment.requests.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class RptIdTest {
    @Test
    fun `empty RptId is invalid`() {
        assertThrows<IllegalArgumentException> {
            RptId("")
        }
    }

    @Test
    fun `RptId too long is rejected`() {
        assertThrows<IllegalArgumentException> {
            RptId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        }
    }

    @Test
    fun `valid RptId is ok`() {
        assertDoesNotThrow {
            RptId("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        }
    }
}