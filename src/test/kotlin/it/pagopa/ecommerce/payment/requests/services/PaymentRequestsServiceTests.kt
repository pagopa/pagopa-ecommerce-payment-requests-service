package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentStatusFaultDto
import it.pagopa.ecommerce.generated.payment.requests.server.model.ValidationFaultDto
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.client.NodoPerPspClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfoRepository
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import it.pagopa.ecommerce.payment.requests.utils.NodoUtils
import it.pagopa.generated.nodoperpsp.model.*
import it.pagopa.generated.nodoperpsp.model.ObjectFactory
import it.pagopa.generated.transactions.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.datatype.DatatypeFactory

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class PaymentRequestsServiceTests {


    private lateinit var paymentRequestsService: PaymentRequestsService

    @Mock
    private lateinit var paymentRequestsInfoRepository: PaymentRequestInfoRepository

    @Mock
    private lateinit var nodoPerPspClient: NodoPerPspClient

    @Mock
    private lateinit var nodeForPspClient: NodeForPspClient

    @Mock
    private lateinit var baseNodoVerificaRPTRequest: NodoVerificaRPT

    @Mock
    private lateinit var baseVerifyPaymentNoticeReq: VerifyPaymentNoticeReq

    @Mock
    private lateinit var nodoOperations: NodoOperations

    @Mock
    private lateinit var nodoUtilities: NodoUtils

    @BeforeEach
    fun init() {
        paymentRequestsService = PaymentRequestsService(
            paymentRequestsInfoRepository,
            nodoPerPspClient,
            nodeForPspClient,
            ObjectFactory(),
            it.pagopa.generated.transactions.model.ObjectFactory(),
            baseNodoVerificaRPTRequest,
            baseVerifyPaymentNoticeReq,
            nodoUtilities,
            nodoOperations
        )
    }

    @Test
    fun `should return payment info from cache`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val paTaxCode = "77777777777"
        val paName = "Pa Name"
        val description = "Payment request description"
        val amount = Integer.valueOf(1000)
        val paymentRequestInfo = PaymentRequestInfo(
            rptIdAsObject, paTaxCode, paName, description, amount, null, true, null, null, false
        )
        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.of(paymentRequestInfo))
        /**
         * test
         */
        val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)

        /**
         * assertions
         */
        assertEquals(rptIdAsString, responseDto.rptId)
        assertEquals(description, responseDto.description)
        assertNull(responseDto.dueDate)
        assertEquals(amount, responseDto.amount)
        assertEquals(paName, responseDto.paName)
        assertEquals(paTaxCode, responseDto.paFiscalCode)
    }

    @Test
    fun `should return payment request info from Nodo VerificaRPT`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val description = "Payment request description"
        val amount = Integer.valueOf(1000)
        val amountForNodo = BigDecimal.valueOf(amount.toLong())
        val verificaRPTRisposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.OK.value()
        val datiPagamento = NodoTipoDatiPagamentoPA()
        datiPagamento.causaleVersamento = description
        datiPagamento.importoSingoloVersamento = amountForNodo
        esitoVerificaRPT.datiPagamentoPA = datiPagamento
        verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRisposta))
        given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

        /**
         * test
         */
        val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        /**
         * assertions
         */
        assertEquals(rptIdAsString, responseDto.rptId)
        assertEquals(description, responseDto.description)
        assertNull(responseDto.dueDate)
        assertEquals(amount, responseDto.amount)
    }

    @Test
    fun `should return payment request info from Nodo VerificaRPTWithEnteBeneficiario`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val paTaxCode = "77777777777"
        val paName = "Pa Name"
        val description = "Payment request description"
        val amount = Integer.valueOf(1000)
        val amountForNodo = BigDecimal.valueOf(amount.toLong())

        val verificaRPTRisposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.OK.value()
        val datiPagamento = NodoTipoDatiPagamentoPA()
        datiPagamento.causaleVersamento = description
        datiPagamento.importoSingoloVersamento = amountForNodo
        val ente = CtEnteBeneficiario()
        ente.denominazioneBeneficiario = paName
        val paId = CtIdentificativoUnivocoPersonaG()
        paId.codiceIdentificativoUnivoco = paTaxCode
        ente.identificativoUnivocoBeneficiario = paId
        datiPagamento.enteBeneficiario = ente
        esitoVerificaRPT.datiPagamentoPA = datiPagamento
        verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRisposta))
        given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

        /**
         * test
         */
        val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        /**
         * assertions
         */
        assertEquals(rptIdAsString, responseDto.rptId)
        assertEquals(description, responseDto.description)
        assertNull(responseDto.dueDate)
        assertEquals(amount, responseDto.amount)
        assertEquals(paName, responseDto.paName)
        assertEquals(paTaxCode, responseDto.paFiscalCode)
    }

    @Test
    fun `should return payment request info from Nodo VerifyPaymentNotice`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val description = "Payment request description"
        val amount = Integer.valueOf(1000)
        val amountForNodo = BigDecimal.valueOf(amount.toLong())

        val verificaRPTRisposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = "PPT_MULTI_BENEFICIARIO"
        esitoVerificaRPT.fault = fault
        verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        val verifyPaymentNotice = VerifyPaymentNoticeRes()
        verifyPaymentNotice.outcome = StOutcome.OK
        verifyPaymentNotice.paymentDescription = description
        val paymentList = CtPaymentOptionsDescriptionList()
        val paymentDescription = CtPaymentOptionDescription()
        paymentDescription.amount = amountForNodo
        paymentList.paymentOptionDescription.add(paymentDescription)
        verifyPaymentNotice.paymentList = paymentList

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRisposta))
        given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
        given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

        /**
         * test
         */
        val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        /**
         * assertions
         */
        assertEquals(rptIdAsString, responseDto.rptId)
        assertEquals(description, responseDto.description)
        assertNull(responseDto.dueDate)
        assertEquals(amount, responseDto.amount)
    }

    @Test
    fun `should return payment ongoing from verifica RPT`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val verificaRPTRisposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = PaymentStatusFaultDto.PPT_PAGAMENTO_IN_CORSO.value
        esitoVerificaRPT.fault = fault
        verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoUtilities.getCodiceIdRpt(any())).willReturn(NodoTipoCodiceIdRPT())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRisposta))
        /**
         * assertions
         */
        assertThrows<NodoErrorException> {
            paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        }
    }

    @Test
    fun `should return payment unknown from verifica RPT`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val verificaRPTRisposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = ValidationFaultDto.PPT_DOMINIO_SCONOSCIUTO.value
        esitoVerificaRPT.fault = fault
        verificaRPTRisposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoUtilities.getCodiceIdRpt(any())).willReturn(NodoTipoCodiceIdRPT())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRisposta))
        /**
         * assertions
         */
        assertThrows<NodoErrorException> {
            paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        }
    }

    @Test
    fun `should return payment info request from nodo with VerifyPaymentNotice with due date`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)
        val paTaxCode = "77777777777"
        val paName = "Pa Name"
        val description = "Payment request description"
        val amount = 1000
        val amountForNodo = BigDecimal.valueOf(amount.toLong())

        val format: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = format.parse("2022-04-24")
        val dueDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(format.format(date))

        val verificaRPTRIsposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = "PPT_MULTI_BENEFICIARIO"
        esitoVerificaRPT.fault = fault
        verificaRPTRIsposta.nodoVerificaRPTRisposta = esitoVerificaRPT

        val verifyPaymentNotice = VerifyPaymentNoticeRes()
        verifyPaymentNotice.outcome = StOutcome.OK
        verifyPaymentNotice.paymentDescription = description
        val paymentList = CtPaymentOptionsDescriptionList()
        val paymentDescription = CtPaymentOptionDescription()
        paymentDescription.amount = amountForNodo
        paymentDescription.dueDate = dueDate
        paymentList.paymentOptionDescription.add(paymentDescription)
        verifyPaymentNotice.paymentList = paymentList

        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRIsposta))
        given(nodeForPspClient.verifyPaymentNotice(any())).willReturn(Mono.just(verifyPaymentNotice))
        given(nodoOperations.getEuroCentsFromNodoAmount(amountForNodo)).willReturn(amount)

        /**
         * test
         */
        val responseDto = paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        /**
         * assertions
         */
        assertEquals(rptIdAsString, responseDto.rptId)
        assertEquals(description, responseDto.description)
        assertEquals(responseDto.dueDate, "2022-04-24")
        assertEquals(amount, responseDto.amount)
    }

    @Test
    fun `should get fault from code`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)

        val verificaRPTRIsposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = "PPT_ERRORE_EMESSO_DA_PAA"

        esitoVerificaRPT.fault = fault
        verificaRPTRIsposta.nodoVerificaRPTRisposta = esitoVerificaRPT


        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRIsposta))

        val exception = assertThrows<NodoErrorException> {
            paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        }
        assertEquals("PPT_ERRORE_EMESSO_DA_PAA", exception.faultCode)
    }

    @Test
    fun `should fallback on fault code on description without fault code`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)

        val verificaRPTRIsposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = "PPT_ERRORE_EMESSO_DA_PAA"
        fault.description = ""

        esitoVerificaRPT.fault = fault
        verificaRPTRIsposta.nodoVerificaRPTRisposta = esitoVerificaRPT


        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRIsposta))

        val exception = assertThrows<NodoErrorException> {
            paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        }
        assertEquals("PPT_ERRORE_EMESSO_DA_PAA", exception.faultCode)
    }

    @Test
    fun `should get fault from description if present`() = runTest {
        val rptIdAsString = "77777777777302016723749670035"
        val rptIdAsObject = RptId(rptIdAsString)

        val verificaRPTRIsposta = NodoVerificaRPTRisposta()
        val esitoVerificaRPT = EsitoNodoVerificaRPTRisposta()
        esitoVerificaRPT.esito = StOutcome.KO.value()
        val fault = FaultBean()
        fault.faultCode = "PPT_ERRORE_EMESSO_DA_PAA"
        fault.description =
            """
            FaultString PA: Pagamento in attesa risulta concluso all’Ente Creditore
            FaultCode PA: PAA_PAGAMENTO_DUPLICATO
            Description PA:   
            """.trimIndent()
        esitoVerificaRPT.fault = fault
        verificaRPTRIsposta.nodoVerificaRPTRisposta = esitoVerificaRPT


        /**
         * preconditions
         */
        given(paymentRequestsInfoRepository.findById(rptIdAsObject)).willReturn(Optional.empty())
        given(nodoPerPspClient.verificaRpt(any())).willReturn(Mono.just(verificaRPTRIsposta))

        val exception = assertThrows<NodoErrorException> {
            paymentRequestsService.getPaymentRequestInfo(rptIdAsString)
        }
        assertEquals("PAA_PAGAMENTO_DUPLICATO", exception.faultCode)
    }


}