package it.pagopa.ecommerce.payment.requests.exceptions

import org.springframework.http.HttpStatusCode

class CheckPositionErrorException(val httpStatus: HttpStatusCode) :
  RuntimeException(
    "Exception communication with nodo checkPosition. http status code: [${httpStatus}]") {}
