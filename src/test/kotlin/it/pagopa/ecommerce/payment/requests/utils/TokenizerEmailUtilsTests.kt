package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import java.util.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

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

    /* preconditions */ given(confidentialDataManager.encrypt(email))
      .willReturn(Mono.just(computedConfidential))
    given(
        confidentialDataManager.decrypt(
          ArgumentMatchers.eq(computedConfidential), ArgumentMatchers.any()))
      .willReturn(Mono.just(email))

    /* test */
    StepVerifier.create(tokenizerEmailUtils.toConfidential(email))
      .expectNext(computedConfidential)
      .verifyComplete()

    StepVerifier.create(tokenizerEmailUtils.toEmail(computedConfidential))
      .expectNext(email)
      .verifyComplete()
  }

  @Test
  fun shouldHandleInvalidEmail() {
    val invalidEmail = "invalidEmail.com"
    assertThrows<IllegalArgumentException> {
      tokenizerEmailUtils.toConfidential(invalidEmail).block()
    }
  }
}
