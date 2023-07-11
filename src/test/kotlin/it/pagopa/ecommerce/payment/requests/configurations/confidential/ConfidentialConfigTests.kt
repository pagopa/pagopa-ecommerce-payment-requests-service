package it.pagopa.ecommerce.payment.requests.configurations.confidential

import it.pagopa.ecommerce.payment.requests.configurations.confidential.ConfidentialConfig
import it.pagopa.ecommerce.payment.requests.configurations.webclients.WebClientsConfig
import it.pagopa.generated.pdv.v1.api.TokenApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ConfidentialConfigTests {
  private val confidentialConfig = ConfidentialConfig()

  @Test
  fun `should return valid emailConfidentialDataManager`() = runTest {
    assertDoesNotThrow {
      confidentialConfig.emailConfidentialDataManager(TokenApi())
    }
  }

  @Test
  fun `should return valid TokenApi`() = runTest {
    assertDoesNotThrow {
      confidentialConfig.personalDataVaultApiClient(UUID.randomUUID().toString(),"base-path")
    }
  }
}
