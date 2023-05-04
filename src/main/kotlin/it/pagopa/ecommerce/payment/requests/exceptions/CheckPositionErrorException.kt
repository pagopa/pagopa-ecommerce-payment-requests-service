package it.pagopa.ecommerce.payment.requests.exceptions

import org.springframework.http.HttpStatus

class CheckPositionErrorException(val httpStatus: HttpStatus) :
  RuntimeException(
    "Exception communication with nodo checkPosition. http status code: [${httpStatus}]") {}
