package it.pagopa.ecommerce.payment.requests.errorhandling

import it.pagopa.ecommerce.generated.payment.requests.server.model.ProblemJsonDto
import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ServerWebInputException
import javax.validation.ValidationException

@RestControllerAdvice
/*
 * Exception handler used to output a custom message in case an incoming request
 * is invalid or an api encounter an error and throw an RestApiException
 */
class ExceptionHandler {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /*
     * Custom rest api exception handler
     */
    @ExceptionHandler(RestApiException::class)
    fun handleException(e: RestApiException): ResponseEntity<ProblemJsonDto> {
        logger.error("Exception processing request", e)
        return ResponseEntity.status(e.httpStatus).body(
            ProblemJsonDto(
                title = e.title,
                detail = e.description,
                status = e.httpStatus.value()
            )
        )
    }

    @ExceptionHandler(ApiError::class)
    fun handleException(e: ApiError): ResponseEntity<ProblemJsonDto> {
        val restApiException = e.toRestException()
        logger.error("Exception processing request", e)
        return ResponseEntity.status(restApiException.httpStatus).body(
            ProblemJsonDto(
                title = restApiException.title,
                detail = restApiException.description,
                status = restApiException.httpStatus.value()
            )
        )
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
        WebExchangeBindException::class
    )
    fun handleRequestValidationException(e: Exception): ResponseEntity<ProblemJsonDto> {
        logger.error("Input request is not valid", e)
        return ResponseEntity.badRequest().body(
            ProblemJsonDto(
                title = "Request validation error",
                detail = "The input request is invalid",
                status = 400

            )
        )
    }

    /*
     * Validation request exception handler
     */
    @ExceptionHandler(
        Exception::class
    )
    fun handleGenericException(e: Exception): ResponseEntity<ProblemJsonDto> {
        logger.error("Unhandled exception", e)
        return ResponseEntity.internalServerError().body(
            ProblemJsonDto(
                title = "Error processing the request",
                detail = "An internal error occurred processing the request",
                status = 500
            )
        )
    }
}