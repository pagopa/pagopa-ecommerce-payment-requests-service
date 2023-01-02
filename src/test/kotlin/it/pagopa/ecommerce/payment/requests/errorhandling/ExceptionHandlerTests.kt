package it.pagopa.ecommerce.payment.requests.errorhandling

import io.mockk.mockkObject
import it.pagopa.ecommerce.payment.requests.exceptions.ValidationFailedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.core.MethodParameter
import org.springframework.test.context.TestPropertySource
import org.springframework.validation.BindingResult
import org.springframework.validation.DirectFieldBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.support.WebExchangeBindException

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class ExceptionHandlerTests {

  private val fieldsToObfuscate = setOf("emailNotice")

  private val validationFailedExceptionCompanionObject: ValidationFailedException.Companion = mock()

  private val exceptionHandler = ExceptionHandler(fieldsToObfuscate)

  @Test
  fun `handle request validation exception should wrap WebExchangeBindException exception`() {
    val bindingResult: BindingResult = DirectFieldBindingResult("", "cartsRequest")
    val fieldError = FieldError("testObject", "field", "testFieldValue", true, null, null, null)
    bindingResult.addError(fieldError)
    given(validationFailedExceptionCompanionObject.fromBindingResult(any(), any()))
      .willReturn(ValidationFailedException("test"))
    val methodParameter =
      MethodParameter.forExecutable(String::class.java.getMethod("toString"), -1)
    val webExchangeBindException = WebExchangeBindException(methodParameter, bindingResult)
    mockkObject(ValidationFailedException.Companion)
    exceptionHandler.handleRequestValidationException(webExchangeBindException)
    io.mockk.verify(exactly = 1) {
      ValidationFailedException.Companion.fromBindingResult(any(), any())
    }
  }

  @Test
  fun `handle request validation exception should wrap MethodArgumentNotValidException exceptions`() {
    val bindingResult: BindingResult = DirectFieldBindingResult("", "cartsRequest")
    val fieldError = FieldError("testObject", "field", "testFieldValue", true, null, null, null)
    bindingResult.addError(fieldError)
    given(validationFailedExceptionCompanionObject.fromBindingResult(any(), any()))
      .willReturn(ValidationFailedException("test"))
    val methodParameter =
      MethodParameter.forExecutable(String::class.java.getMethod("toString"), -1)
    val methodArgumentNotValidException =
      MethodArgumentNotValidException(methodParameter, bindingResult)
    mockkObject(ValidationFailedException.Companion)
    exceptionHandler.handleRequestValidationException(methodArgumentNotValidException)
    io.mockk.verify(exactly = 1) {
      ValidationFailedException.Companion.fromBindingResult(any(), any())
    }
  }
}
