package it.pagopa.ecommerce.payment.requests.exceptions

import org.springframework.http.HttpStatus

/*
 * Custom exception used by controllers and captured by ExceptionHandler. This exception values, when catch by exception
 * handler is, are used to construct the correct ProblemJson to output
 */
class RestApiException(val httpStatus: HttpStatus, val title: String, val description: String) :
  RuntimeException(title) {}
