package it.pagopa.ecommerce.payment.requests.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdempotencyKeyTests {

    companion object {
        const val VALID_FISCAL_CODE = "32009090901"
        const val INVALID_FISCAL_CODE = "3200909090"
        const val VALID_KEY_ID = "aabbccddee"
        const val INVALID_KEY_ID = "aabbccddeeffgg"
    }


    @Test
    fun `should throw invalid fiscal code`() {
        val exception = assertThrows<IllegalArgumentException> {
            IdempotencyKey(INVALID_FISCAL_CODE, VALID_KEY_ID)
        }
        assertEquals("PSP fiscal code doesn't match regex: \\d{11}", exception.message)
    }


    @Test
    fun `should throw invalid key id`() {
        val exception = assertThrows<IllegalArgumentException> {
            IdempotencyKey(VALID_FISCAL_CODE, INVALID_KEY_ID)
        }
        assertEquals("Key identifier doesn't match regex: [a-zA-Z\\d]{10}", exception.message)
    }

    @Test
    fun `should return key`() {
        val idempotencyKey = IdempotencyKey(VALID_FISCAL_CODE, VALID_KEY_ID)
        assertTrue(idempotencyKey.key == VALID_FISCAL_CODE + "_" + VALID_KEY_ID)
    }

    @Test
    fun `should generate same key`() {
        val key1 = IdempotencyKey(VALID_FISCAL_CODE, VALID_KEY_ID)
        val key2 = IdempotencyKey(VALID_FISCAL_CODE + "_" + VALID_KEY_ID)
        assertEquals(key1, key2)
        assertEquals(key1, key1)
    }
}