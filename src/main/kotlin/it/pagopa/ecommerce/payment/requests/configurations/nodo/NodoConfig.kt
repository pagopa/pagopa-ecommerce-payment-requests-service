package it.pagopa.ecommerce.payment.requests.configurations.nodo

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.payment.requests.configurations.nodo.util.NodoConnectionString
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NodoConfig(@Value("\${nodo.connection.string}") private val connectionString: String) {

  private val objectMapper = ObjectMapper()

  companion object Constants {
    const val PSP_INFRASTRUCTURE_CODIFICATION = "QR-CODE"
  }

  @Bean
  fun nodoConnectionString(): NodoConnectionString =
    objectMapper.readValue(connectionString, NodoConnectionString::class.java)

  fun baseVerifyPaymentNoticeReq(): VerifyPaymentNoticeReq {
    val objectFactory = it.pagopa.ecommerce.generated.transactions.model.ObjectFactory()
    val request: VerifyPaymentNoticeReq = objectFactory.createVerifyPaymentNoticeReq()
    val nodoConnectionParams = nodoConnectionString()
    request.idPSP = nodoConnectionParams.idPSP
    request.idChannel = nodoConnectionParams.idChannel
    request.idBrokerPSP = nodoConnectionParams.idBrokerPSP
    request.password = nodoConnectionParams.password
    return request
  }
}
