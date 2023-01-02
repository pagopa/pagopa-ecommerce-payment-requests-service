package it.pagopa.ecommerce.payment.requests.exceptions

import java.util.regex.Pattern
import org.springframework.util.ObjectUtils
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError

class ValidationFailedException(errorMessage: String) : RuntimeException(errorMessage) {

  companion object {
    fun fromBindingResult(
      bindingResult: BindingResult,
      fieldsToObfuscate: Set<String>
    ): ValidationFailedException {
      val fieldSearchPattern: Pattern =
        Pattern.compile(".*%s.*".format(fieldsToObfuscate.joinToString(separator = "|")))
      return ValidationFailedException(logErrors(bindingResult, fieldSearchPattern))
    }

    private fun logErrors(bindingResult: BindingResult, fieldSearchPattern: Pattern): String {
      val toLog =
        StringBuilder(
          "Input request validation exception. Error count: (${bindingResult.allErrors.size})")
      toLog.append(System.lineSeparator())
      for (error in bindingResult.allErrors) {
        if (error is FieldError) {
          toLog.append(logField(error, fieldSearchPattern))
        } else {
          toLog.append(error.toString())
        }
        toLog.append(System.lineSeparator())
      }
      return toLog.toString()
    }

    private fun logField(fieldError: FieldError, fieldSearchPattern: Pattern): String {
      val rejectedValue: Any? =
        if (fieldSearchPattern.matcher(fieldError.field).find()) {
          "***OBFUSCATED***"
        } else {
          fieldError.rejectedValue
        }
      return "Field [${fieldError.field}]: rejected value [${ObjectUtils.nullSafeToString(rejectedValue)}]; reason: [${fieldError.defaultMessage}]"
    }
  }
}
