package it.pagopa.ecommerce.payment.requests.errorhandling

import io.lettuce.core.RedisConnectionException
import it.pagopa.ecommerce.generated.payment.requests.server.model.*
import it.pagopa.ecommerce.payment.requests.exceptions.CheckPositionErrorException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import it.pagopa.ecommerce.payment.requests.exceptions.ValidationFailedException
import java.util.*
import javax.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.RedisSystemException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
/*
 * Exception handler used to output a custom message in case an incoming request
 * is invalid or an api encounter an error and throw an RestApiException
 */
class ExceptionHandler(@Value("#{\${fields_to_obscure}}") val fieldToObscure: Set<String>) {

  val logger: Logger = LoggerFactory.getLogger(javaClass)

  /*
   * Custom rest api exception handler
   */
  @ExceptionHandler(RestApiException::class)
  fun handleException(e: RestApiException): ResponseEntity<ProblemJsonDto> {
    logger.error("Exception processing request", e)
    return ResponseEntity.status(e.httpStatus)
      .body(ProblemJsonDto(title = e.title, detail = e.description, status = e.httpStatus.value()))
  }

  @ExceptionHandler(CheckPositionErrorException::class)
  fun handleException(e: CheckPositionErrorException): ResponseEntity<ProblemJsonDto> {
    logger.error("Nodo error checkPosition request", e)
    val response: ResponseEntity<ProblemJsonDto> =
      when (e.httpStatus) {
        HttpStatus.INTERNAL_SERVER_ERROR ->
          ResponseEntity(
            ProblemJsonDto(status = HttpStatus.BAD_GATEWAY.value(), title = "Bad gateway"),
            HttpStatus.BAD_GATEWAY)
        HttpStatus.UNPROCESSABLE_ENTITY ->
          ResponseEntity(
            ProblemJsonDto(
              status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
              title = "Invalid payment info",
              detail = "Invalid payment notice data"),
            HttpStatus.UNPROCESSABLE_ENTITY)
        else ->
          ResponseEntity(
            ProblemJsonDto(
              status = HttpStatus.INTERNAL_SERVER_ERROR.value(), title = "Internal server error"),
            HttpStatus.INTERNAL_SERVER_ERROR)
      }
    return response
  }

  @ExceptionHandler(ApiError::class)
  fun handleException(e: ApiError): ResponseEntity<ProblemJsonDto> {
    val restApiException = e.toRestException()
    logger.error("Exception processing request", e)
    return ResponseEntity.status(restApiException.httpStatus)
      .body(
        ProblemJsonDto(
          title = restApiException.title,
          detail = restApiException.description,
          status = restApiException.httpStatus.value()))
  }

  /*
   * Validation request exception handler
   */
  @ExceptionHandler(
    MethodArgumentNotValidException::class,
    MethodArgumentTypeMismatchException::class,
    ServerWebInputException::class,
    ValidationException::class,
    HttpMessageNotReadableException::class,
    WebExchangeBindException::class)
  fun handleRequestValidationException(e: Exception): ResponseEntity<ProblemJsonDto> {
    val bindingResult: BindingResult? =
      when (e) {
        is WebExchangeBindException -> e.bindingResult
        is MethodArgumentNotValidException -> e.bindingResult
        else -> null
      }
    val exceptionToLog =
      if (bindingResult != null) {
        ValidationFailedException.fromBindingResult(bindingResult, fieldToObscure)
      } else {
        e
      }
    logger.error("Input request is not valid", exceptionToLog)
    return ResponseEntity.badRequest()
      .body(
        ProblemJsonDto(
          title = "Request validation error",
          detail = "The input request is invalid",
          status = 400))
  }

  @ExceptionHandler(
    RedisSystemException::class, RedisConnectionException::class, WebClientRequestException::class)
  fun genericBadGateweyHandler(e: Exception): ResponseEntity<ProblemJsonDto> {
    logger.error("Error processing request", e)
    return ResponseEntity(
      ProblemJsonDto(status = HttpStatus.BAD_GATEWAY.value(), title = "Bad gateway"),
      HttpStatus.BAD_GATEWAY)
  }

  // error codes
  val nodeErrorToResponseEntityMapping: Map<String, ResponseEntity<*>> =
  // nodo error code to 404 response mapping
  ValidationFaultPaymentUnknownDto.values().associate {
      Pair(
        it.toString(),
        ResponseEntity(
          ValidationFaultPaymentUnknownProblemJsonDto(
            title = "Payment Status Fault",
            faultCodeCategory =
              ValidationFaultPaymentUnknownProblemJsonDto.FaultCodeCategory.PAYMENT_UNKNOWN,
            faultCodeDetail = it),
          HttpStatus.NOT_FOUND))
    } +
      ValidationFaultPaymentDataErrorDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            ValidationFaultPaymentDataErrorProblemJsonDto(
              title = "Payment Status Fault",
              faultCodeCategory =
                ValidationFaultPaymentDataErrorProblemJsonDto.FaultCodeCategory.PAYMENT_DATA_ERROR,
              faultCodeDetail = it),
            HttpStatus.NOT_FOUND))
      } +
      // nodo error code to 409 response mapping
      PaymentOngoingStatusFaultDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            PaymentOngoingStatusFaultPaymentProblemJsonDto(
              title = "Payment Status Fault",
              faultCodeCategory =
                PaymentOngoingStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_ONGOING,
              faultCodeDetail = it),
            HttpStatus.CONFLICT))
      } +
      PaymentExpiredStatusFaultDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            PaymentExpiredStatusFaultPaymentProblemJsonDto(
              title = "Payment Status Fault",
              faultCodeCategory =
                PaymentExpiredStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_EXPIRED,
              faultCodeDetail = it),
            HttpStatus.CONFLICT))
      } +
      PaymentCanceledStatusFaultDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            PaymentCanceledStatusFaultPaymentProblemJsonDto(
              title = "Payment Status Fault",
              faultCodeCategory =
                PaymentCanceledStatusFaultPaymentProblemJsonDto.FaultCodeCategory.PAYMENT_CANCELED,
              faultCodeDetail = it),
            HttpStatus.CONFLICT))
      } +
      PaymentDuplicatedStatusFaultDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            PaymentDuplicatedStatusFaultPaymentProblemJsonDto(
              title = "Payment Status Fault",
              faultCodeCategory =
                PaymentDuplicatedStatusFaultPaymentProblemJsonDto.FaultCodeCategory
                  .PAYMENT_DUPLICATED,
              faultCodeDetail = it),
            HttpStatus.CONFLICT))
      } +
      // nodo error code to 502 response mapping
      ValidationFaultPaymentUnavailableDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            ValidationFaultPaymentUnavailableProblemJsonDto(
              title = "Payment unavailable",
              faultCodeCategory =
                ValidationFaultPaymentUnavailableProblemJsonDto.FaultCodeCategory
                  .PAYMENT_UNAVAILABLE,
              faultCodeDetail = it),
            HttpStatus.BAD_GATEWAY))
      } +
      // nodo error code to 503 response mapping
      PartyConfigurationFaultDto.values().associate {
        Pair(
          it.toString(),
          ResponseEntity(
            PartyConfigurationFaultPaymentProblemJsonDto(
              title = "EC error",
              faultCodeCategory =
                PartyConfigurationFaultPaymentProblemJsonDto.FaultCodeCategory.DOMAIN_UNKNOWN,
              faultCodeDetail = it),
            HttpStatus.SERVICE_UNAVAILABLE))
      }

  @ExceptionHandler(
    NodoErrorException::class,
  )
  fun nodoErrorHandler(e: NodoErrorException): ResponseEntity<*> {
    val faultCode = e.faultCode
    val response =
      nodeErrorToResponseEntityMapping[faultCode]
        ?: ResponseEntity(
          GatewayFaultPaymentProblemJsonDto(
            title = "Bad gateway",
            faultCodeCategory = GatewayFaultPaymentProblemJsonDto.FaultCodeCategory.GENERIC_ERROR,
            faultCodeDetail = faultCode),
          HttpStatus.BAD_GATEWAY)

    logger.error(
      "Nodo error processing request with fault code: [$faultCode] mapped to http status code: [${response.statusCode}]",
      e)
    return response
  }

  /*
   * Validation request exception handler
   */
  @ExceptionHandler(Exception::class)
  fun handleGenericException(e: Exception): ResponseEntity<ProblemJsonDto> {
    logger.error("Unhandled exception", e)
    return ResponseEntity.internalServerError()
      .body(
        ProblemJsonDto(
          title = "Error processing the request",
          detail = "An internal error occurred processing the request",
          status = 500))
  }
}
