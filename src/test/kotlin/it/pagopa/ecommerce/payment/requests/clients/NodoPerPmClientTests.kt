package it.pagopa.ecommerce.payment.requests.clients

import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.AvanzamentoPagamentoDto.EsitoEnum
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.CheckPositionResponseDto
import it.pagopa.ecommerce.generated.nodoperpm.v1.dto.ListelementRequestDto
import it.pagopa.ecommerce.generated.transactions.model.ObjectFactory
import it.pagopa.ecommerce.payment.requests.client.NodoPerPmClient
import it.pagopa.ecommerce.payment.requests.exceptions.CheckPositionErrorException
import it.pagopa.ecommerce.payment.requests.utils.client.ResponseSpecCustom
import java.util.function.Function
import java.util.function.Predicate
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class NodoPerPmClientTests {
  private lateinit var client: NodoPerPmClient

  @Mock private lateinit var nodoWebClient: WebClient

  @Mock private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

  @Mock private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>

  @Mock private lateinit var responseSpec: WebClient.ResponseSpec

  @Mock private lateinit var customResponseSpec: ResponseSpecCustom

  @BeforeEach
  fun init() {
    client = NodoPerPmClient("", nodoWebClient)
  }

  @Test
  fun `should return verify payment response given valid payment notice`() = runTest {
    val checkPositionDto =
      CheckPositionDto()
        .positionslist(
          listOf(
            ListelementRequestDto().fiscalCode("77777777777").noticeNumber("303312387654312381")))
    val objectFactory = ObjectFactory()
    val response = CheckPositionResponseDto().outcome(CheckPositionResponseDto.OutcomeEnum.OK)
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(CheckPositionDto::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatusCode>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.bodyToMono(CheckPositionResponseDto::class.java))
      .willReturn(Mono.just(response))

    /** test */
    val testResponse = client.checkPosition(checkPositionDto).block()

    /** asserts */
    Assertions.assertThat(testResponse!!.outcome.value).isEqualTo(EsitoEnum.OK.value)
  }

  @Test
  fun `should return checkPosition Rest api exception`() = runTest {
    val checkPositionDto =
      CheckPositionDto()
        .positionslist(
          listOf(
            ListelementRequestDto().fiscalCode("77777777777").noticeNumber("303312387654312381")))
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(CheckPositionDto::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(customResponseSpec)
    given(customResponseSpec.status).willReturn(HttpStatus.BAD_REQUEST)
    given(
        customResponseSpec.onStatus(
          any<Predicate<HttpStatusCode>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willCallRealMethod()

    assertThrows<CheckPositionErrorException> { client.checkPosition(checkPositionDto) }
    /** test */
  }
}
