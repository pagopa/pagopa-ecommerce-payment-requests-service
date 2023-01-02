package it.pagopa.ecommerce.payment.requests.configurations.nodo

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.generated.nodoperpsp.model.NodoVerificaRPT
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.payment.requests.configurations.nodo.util.NodoConnectionString
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NodoConfig {

  private val objectMapper = ObjectMapper()

  companion object Constants {
    const val PSP_INFRASTRUCTURE_CODIFICATION = "QR-CODE"
  }

  @Bean
  fun nodoConnectionString(
    @Value("\${nodo.connection.string}") connectionString: String
  ): NodoConnectionString =
    objectMapper.readValue(connectionString, NodoConnectionString::class.java)

  @Bean
  fun baseNodoVerificaRPTRequest(
    @Value("\${nodo.connection.string}") connectionString: String
  ): NodoVerificaRPT {
    val objectFactory = it.pagopa.ecommerce.generated.nodoperpsp.model.ObjectFactory()
    val request: NodoVerificaRPT = objectFactory.createNodoVerificaRPT()
    val nodoConnectionParams = nodoConnectionString(connectionString)
    request.identificativoPSP = nodoConnectionParams.idPSP
    request.identificativoCanale = nodoConnectionParams.idChannel
    request.identificativoIntermediarioPSP = nodoConnectionParams.idBrokerPSP
    request.password = nodoConnectionParams.password
    request.codificaInfrastrutturaPSP = PSP_INFRASTRUCTURE_CODIFICATION
    return request
  }

  @Bean
  fun baseVerifyPaymentNoticeReq(
    @Value("\${nodo.connection.string}") connectionString: String
  ): VerifyPaymentNoticeReq {
    val objectFactory = it.pagopa.ecommerce.generated.transactions.model.ObjectFactory()
    val request: VerifyPaymentNoticeReq = objectFactory.createVerifyPaymentNoticeReq()
    val nodoConnectionParams = nodoConnectionString(connectionString)
    request.idPSP = nodoConnectionParams.idPSP
    request.idChannel = nodoConnectionParams.idChannel
    request.idBrokerPSP = nodoConnectionParams.idBrokerPSP
    request.password = nodoConnectionParams.password
    return request
  }
}
