package it.pagopa.ecommerce.payment.requests.errorhandling

import it.pagopa.ecommerce.generated.payment.requests.server.model.*
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
/*
 * Exception handler used to output a custom message in case an incoming request
 * is invalid or an api encounter an error and throw an RestApiException
 */
class ExceptionHandler(@Value("#{\${fields_to_obscure}}") val fieldToObscure: Set<String>) {

  val logger: Logger = LoggerFactory.getLogger(javaClass)

  /*
   * Validation request exception handler
   */
  @ExceptionHandler(Exception::class)
  fun handleGenericException(e: Exception): ResponseEntity<ProblemJsonDto> {
    logger.error("Unhandled exception", e)
    return ResponseEntity.internalServerError()
      .body(
        ProblemJsonDto()
          .title("Error processing the request")
          .detail("An internal error occurred processing the request")
          .status(500))
  }
}
