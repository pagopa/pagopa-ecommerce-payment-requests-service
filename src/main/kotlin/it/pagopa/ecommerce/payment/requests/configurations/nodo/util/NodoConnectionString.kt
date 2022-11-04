package it.pagopa.ecommerce.payment.requests.configurations.nodo.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class NodoConnectionString(
    @JsonProperty("idPSP") val idPSP: String,
    @JsonProperty("idChannel") val idChannel: String,
    @JsonProperty("idChannelPayment") val idChannelPayment: String,
    @JsonProperty("idBrokerPSP") val idBrokerPSP: String,
    @JsonProperty("password") val password: String
)