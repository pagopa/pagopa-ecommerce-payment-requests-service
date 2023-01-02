package it.pagopa.ecommerce.payment.requests.clients

import it.pagopa.ecommerce.generated.nodoperpsp.model.NodoVerificaRPTRisposta
import it.pagopa.ecommerce.payment.requests.client.NodoPerPspClient
import it.pagopa.ecommerce.payment.requests.utils.soap.SoapEnvelope
import java.math.BigDecimal
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.*
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class NodPerPspClientTests {

  private lateinit var client: NodoPerPspClient

  @Mock private lateinit var nodoWebClient: WebClient

  @Mock private lateinit var requestBodyUriSpec: RequestBodyUriSpec

  @Mock private lateinit var requestHeadersSpec: RequestHeadersSpec<*>

  @Mock private lateinit var responseSpec: ResponseSpec

  @BeforeEach
  fun init() {
    client = NodoPerPspClient("", nodoWebClient)
  }

  @Test
  fun `should return verify payment response given valid payment notice`() = runTest {
    val objectFactory = it.pagopa.ecommerce.generated.nodoperpsp.model.ObjectFactory()
    val ccp = UUID.randomUUID().toString()
    val codificaInfrastrutturaPSP = "codificaInfrastrutturaPSP"
    val identificativoPSP = "identificativoPSP"
    val identificativoCanale = "identificativoCanale"
    val password = "password"
    val identificativoIntermediarioPSP = "identificativoIntermediarioPSP"
    val esito = "OK"
    val importoSingoloVersamento = BigDecimal.valueOf(1200)
    val causaleVersamento = "causaleVersamento"

    val request = objectFactory.createNodoVerificaRPT()
    request.codiceContestoPagamento = ccp
    request.codificaInfrastrutturaPSP = codificaInfrastrutturaPSP
    request.identificativoPSP = identificativoPSP
    request.identificativoCanale = identificativoCanale
    request.password = password
    request.identificativoIntermediarioPSP = identificativoIntermediarioPSP

    val verificaRPTRisposta = objectFactory.createNodoVerificaRPTRisposta()
    val esitoVerifica = objectFactory.createEsitoNodoVerificaRPTRisposta()
    esitoVerifica.esito = esito
    val datiPagamento = objectFactory.createNodoTipoDatiPagamentoPA()
    datiPagamento.importoSingoloVersamento = importoSingoloVersamento
    datiPagamento.causaleVersamento = causaleVersamento
    esitoVerifica.datiPagamentoPA = datiPagamento
    verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerifica
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(SoapEnvelope::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.bodyToMono(NodoVerificaRPTRisposta::class.java))
      .willReturn(Mono.just(verificaRPTRisposta))

    /** test */
    val testResponse = client.verificaRpt(objectFactory.createNodoVerificaRPT(request)).block()

    /** asserts */
    assertThat(testResponse?.nodoVerificaRPTRisposta?.esito).isEqualTo(esito)
    assertThat(testResponse?.nodoVerificaRPTRisposta?.datiPagamentoPA?.importoSingoloVersamento)
      .isEqualTo(importoSingoloVersamento)
    assertThat(testResponse?.nodoVerificaRPTRisposta?.datiPagamentoPA?.causaleVersamento)
      .isEqualTo(causaleVersamento)
  }

  @Test
  fun `should return verify fault given duplicate payment notice`() = runTest {
    val objectFactory = it.pagopa.ecommerce.generated.nodoperpsp.model.ObjectFactory()
    val ccp = UUID.randomUUID().toString()
    val codificaInfrastrutturaPSP = "codificaInfrastrutturaPSP"
    val identificativoPSP = "identificativoPSP"
    val identificativoCanale = "identificativoCanale"
    val password = "password"
    val identificativoIntermediarioPSP = "identificativoIntermediarioPSP"
    val esito = "KO"
    val faultError = "PAA_PAGAMENTO_DUPLICATO"

    val request = objectFactory.createNodoVerificaRPT()
    request.codiceContestoPagamento = ccp
    request.codificaInfrastrutturaPSP = codificaInfrastrutturaPSP
    request.identificativoPSP = identificativoPSP
    request.identificativoCanale = identificativoCanale
    request.password = password
    request.identificativoIntermediarioPSP = identificativoIntermediarioPSP

    val verificaRPTRisposta = objectFactory.createNodoVerificaRPTRisposta()
    val esitoVerifica = objectFactory.createEsitoNodoVerificaRPTRisposta()
    esitoVerifica.esito = esito
    val faultBean = objectFactory.createFaultBean()
    faultBean.faultCode = faultError
    faultBean.faultString = faultError
    esitoVerifica.fault = faultBean
    verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerifica
    /** precondition */
    given(nodoWebClient.post()).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.uri(any<String>(), any<Array<*>>())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.header(any(), any())).willReturn(requestBodyUriSpec)
    given(requestBodyUriSpec.body(any(), eq(SoapEnvelope::class.java)))
      .willReturn(requestHeadersSpec)
    given(requestHeadersSpec.retrieve()).willReturn(responseSpec)
    given(
        responseSpec.onStatus(
          any<Predicate<HttpStatus>>(), any<Function<ClientResponse, Mono<out Throwable>>>()))
      .willReturn(responseSpec)
    given(responseSpec.bodyToMono(NodoVerificaRPTRisposta::class.java))
      .willReturn(Mono.just(verificaRPTRisposta))

    /** test */
    val testResponse = client.verificaRpt(objectFactory.createNodoVerificaRPT(request)).block()

    /** asserts */
    assertThat(testResponse?.nodoVerificaRPTRisposta?.esito).isEqualTo(esito)
    assertThat(testResponse?.nodoVerificaRPTRisposta?.fault?.faultCode).isEqualTo(faultError)
    assertThat(testResponse?.nodoVerificaRPTRisposta?.fault?.faultString).isEqualTo(faultError)
  }
}
