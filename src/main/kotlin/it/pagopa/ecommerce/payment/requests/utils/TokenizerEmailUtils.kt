package it.pagopa.ecommerce.payment.requests.utils

import it.pagopa.ecommerce.payment.requests.mdcutilities.LogTracingUtils
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
constructor(private val emailConfidentialDataManager: ConfidentialDataManager) {
  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  fun toEmail(tokenizedEmail: Confidential<Email>): Mono<Email> {
    return emailConfidentialDataManager
      .decrypt(tokenizedEmail) { Email(it) }
      .doOnError { e ->
        LogTracingUtils.loggerTracingUtils()
          .failure()
          .logError(logger, e, "Exception get mail from tokenized email")
      }
  }

  fun toConfidential(clearText: Email): Mono<Confidential<Email>> {
    return emailConfidentialDataManager.encrypt(clearText).doOnError { e ->
      LogTracingUtils.loggerTracingUtils()
        .failure()
        .logError(logger, e, "Exception tokenizing confidential data")
    }
  }

  fun toConfidential(email: String?): Mono<Confidential<Email>> {
    return toConfidential(Email(email))
  }
}
