package it.pagopa.ecommerce.payment.requests.configurations.confidential

import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager
import it.pagopa.generated.pdv.v1.ApiClient
import it.pagopa.generated.pdv.v1.api.TokenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConfidentialConfig {

  @Bean
  fun personalDataVaultApiClient(
    @Value("\${confidentialDataManager.personalDataVault.apiKey}") personalDataVaultApiKey: String,
    @Value("\${confidentialDataManager.personalDataVault.apiBasePath}") apiBasePath: String?
  ): TokenApi {
    val pdvApiClient = ApiClient()
    pdvApiClient.setApiKey(personalDataVaultApiKey)
    pdvApiClient.basePath = apiBasePath
    return TokenApi(pdvApiClient)
  }

  @Bean
  fun emailConfidentialDataManager(personalDataVaultApi: TokenApi?): ConfidentialDataManager {
    return ConfidentialDataManager(personalDataVaultApi)
  }
}
