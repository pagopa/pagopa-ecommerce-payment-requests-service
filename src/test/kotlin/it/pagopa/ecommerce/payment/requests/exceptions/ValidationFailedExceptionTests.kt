package it.pagopa.ecommerce.payment.requests.exceptions

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.validation.BindingResult
import org.springframework.validation.DirectFieldBindingResult
import org.springframework.validation.FieldError

class ValidationFailedExceptionTests {

  private val fieldsToObfuscate = setOf("emailNotice")

  @Test
  fun `validation request should not log sensitive data`() {
    val bindingResult: BindingResult = DirectFieldBindingResult("", "cartsRequest")
    val fieldErrorMail = FieldError("", "emailNotice", "test@test.it.", true, null, null, null)
    val fieldErrorFiscalCode =
      FieldError("", "fiscalCode", "testFiscalCode", true, null, null, null)
    bindingResult.addError(fieldErrorMail)
    bindingResult.addError(fieldErrorFiscalCode)
    val validationFailedException =
      ValidationFailedException.fromBindingResult(bindingResult, fieldsToObfuscate)
    println(validationFailedException.message)
    val emailPresent: Boolean =
      validationFailedException.message?.contains("rejected value [test@test.it.]") ?: false
    val fiscalCodePresent: Boolean =
      validationFailedException.message?.contains("rejected value [testFiscalCode]") ?: false
    assertTrue(!emailPresent)
    assertTrue(fiscalCodePresent)
  }
}
