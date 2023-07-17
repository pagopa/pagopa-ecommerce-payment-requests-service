package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Slf4j
class TokenizerEmailUtils
@Autowired
constructor(emailConfidentialDataManager: ConfidentialDataManager) {
  private val emailConfidentialDataManager: ConfidentialDataManager
  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  init {
    this.emailConfidentialDataManager = emailConfidentialDataManager
  }

  fun toEmail(tokenizedEmail: Confidential<Email>): Mono<Email> {
    return emailConfidentialDataManager
      .decrypt(tokenizedEmail) { Email(it) }
      .doOnError { e -> logger.error("Exception get mail from tokenized email", e) }
  }

  fun toConfidential(clearText: Email): Mono<Confidential<Email>> {
    return emailConfidentialDataManager.encrypt(clearText).doOnError { e ->
      logger.error("Exception tokenizing confidential data", e)
    }
  }

  fun toConfidential(email: String?): Mono<Confidential<Email>> {
    return toConfidential(Email(email))
  }
}
