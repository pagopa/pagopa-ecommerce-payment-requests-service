package it.pagopa.ecommerce.payment.requests.exceptions

import it.pagopa.ecommerce.payment.requests.errorhandling.ApiError
import org.springframework.http.HttpStatus

class InvalidRptException(rptId: String) : ApiError("Invalid input rpt id: [$rptId]") {
    override fun toRestException(): RestApiException =
        RestApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            title = "Invalid RPT id",
            description = this.message ?: ""
        )

}