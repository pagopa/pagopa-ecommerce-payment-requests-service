package it.pagopa.ecommerce.payment.requests.errorhandling

import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException

/**
 * Class that bridges business-related exception to `RestException`. Business-related exceptions
 * should extend this class.
 */
abstract class ApiError(message: String?) : RuntimeException(message) {
  abstract fun toRestException(): RestApiException
}
