package it.pagopa.ecommerce.payment.requests.configurations.redis

import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdReadingByteConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdReadingStringConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdWritingByteConverter
import it.pagopa.ecommerce.payment.requests.configurations.redis.converters.rptId.RptIdWritingStringConverter
import it.pagopa.ecommerce.payment.requests.domain.RptId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RedisConfigurationTests {

    private val rptIdReadingByteConverter = RptIdReadingByteConverter()
    private val rptIdReadingStringConverter = RptIdReadingStringConverter()
    private val rptIdWritingByteConverter = RptIdWritingByteConverter()
    private val rptIdWritingStringConverter = RptIdWritingStringConverter()

    @Test
    fun `should return valid configuration`() = runTest {
        assertDoesNotThrow {
            RedisConfiguration().redisCustomConversions(
                rptIdReadingByteConverter,
                rptIdReadingStringConverter,
                rptIdWritingByteConverter,
                rptIdWritingStringConverter
            )
        }
    }

    @Test
    fun `should convert RPT id from and to byte`() = runTest {
        val rptIdString = "77777777777302000100000009424"
        val byteArray = rptIdWritingByteConverter.convert(RptId(rptIdString))
        val rptId = rptIdReadingByteConverter.convert(byteArray)
        assertEquals(rptIdString, rptId.value)
    }

    @Test
    fun `should convert RPT id from and to string`() = runTest {
        val rptIdString = "77777777777302000100000009424"
        val stringConversion = rptIdWritingStringConverter.convert(RptId(rptIdString))
        val rptId = rptIdReadingStringConverter.convert(stringConversion)
        assertEquals(rptIdString, rptId.value)
    }
}
