package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import java.util.*
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import reactor.core.publisher.Mono

class TokenizerEmailUtilsTests {

  private val confidentialDataManager: ConfidentialDataManager =
    Mockito.mock(ConfidentialDataManager::class.java)

  private val tokenizerEmailUtils: TokenizerEmailUtils =
    TokenizerEmailUtils(confidentialDataManager)

  @Test
  fun shouldEncryptAndDecryptMailSuccessfully() {
    val email = Email("test@test.com")
    val emailToken = UUID.randomUUID()
    val computedConfidential: Confidential<Email> = Confidential(emailToken.toString())

    /* preconditions */ Mockito.`when`(confidentialDataManager.encrypt(email))
      .thenReturn(Mono.just(computedConfidential))
    Mockito.`when`(
        confidentialDataManager.decrypt(
          ArgumentMatchers.eq(computedConfidential), ArgumentMatchers.any()))
      .thenReturn(Mono.just(email))

    /* test */
    val encrypted: Confidential<Email> = tokenizerEmailUtils.toConfidential(email).block()!!
    val decrypted: Email = tokenizerEmailUtils.toEmail(encrypted).block()!!

    /* assert */ assertEquals(email, decrypted)
  }

  @Test
  fun shouldHandleInvalidEmail() {

    val invalidEmail = "invalidEmail.com"
    assertThrows<IllegalArgumentException> {
      tokenizerEmailUtils.toConfidential(invalidEmail).block()!!
    }
    assertThrows<IllegalArgumentException> { Email("validEmail") }
  }
}
