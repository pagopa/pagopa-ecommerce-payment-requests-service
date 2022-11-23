package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpsp.model.*
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.generated.transactions.model.CtQrCode
import it.pagopa.ecommerce.generated.transactions.model.StOutcome
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.client.NodoPerPspClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfoRepository
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import it.pagopa.ecommerce.payment.requests.utils.NodoUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar

@Service
class PaymentRequestsService(
    @Autowired private val paymentRequestInfoRepository: PaymentRequestInfoRepository,

    @Autowired private val nodoPerPspClient: NodoPerPspClient,

    @Autowired private val nodeForPspClient: NodeForPspClient,

    @Autowired private val objectFactoryNodoPerPsp: ObjectFactory,

    @Autowired private val objectFactoryNodeForPsp: it.pagopa.ecommerce.generated.transactions.model.ObjectFactory,

    @Autowired private val baseNodoVerificaRPTRequest: NodoVerificaRPT,

    @Autowired private val baseVerifyPaymentNoticeReq: VerifyPaymentNoticeReq,

    @Autowired private val nodoUtils: NodoUtils,

    @Autowired private val nodoOperations: NodoOperations,

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE = "PPT_MULTI_BENEFICIARIO"
    }


    suspend fun getPaymentRequestInfo(rptId: String): PaymentRequestsGetResponseDto {
        logger.info("Retrieving payment info for RPT id: {}", rptId)
        val rptIdRecord = RptId(rptId)
        val paymentContextCode = UUID.randomUUID().toString().replace("-", "")
        val paymentInfoFromCache = getPaymentInfoFromCache(rptIdRecord)
        val paymentInfo: PaymentRequestInfo
        if (paymentInfoFromCache.isPresent) {
            paymentInfo = paymentInfoFromCache.get()
        } else {
            paymentInfo = getPaymentInfoFromNodo(rptIdRecord, paymentContextCode)
            logger.info("PaymentRequestInfo retrieved from nodo pagoPA for {}, isNM3 {}", rptId, paymentInfo.isNM3)
            withContext(dispatcher) {
                paymentRequestInfoRepository.save(paymentInfo)
            }
        }
        logger.info("PaymentRequestInfo retrieved for {}", rptId)
        return PaymentRequestsGetResponseDto(
            rptId = paymentInfo.id.value,
            paFiscalCode = paymentInfo.paFiscalCode,
            paName = paymentInfo.paName,
            dueDate = paymentInfo.dueDate,
            description = paymentInfo.description,
            amount = paymentInfo.amount,
            paymentContextCode = paymentContextCode
        )
    }

    suspend fun getPaymentInfoFromCache(rptId: RptId): Optional<PaymentRequestInfo> {
        val paymentRequestInfoOptional: Optional<PaymentRequestInfo> = paymentRequestInfoRepository.findById(rptId)
        logger.info("PaymentRequestInfo cache hit for {}: {}", rptId, paymentRequestInfoOptional.isPresent)
        return paymentRequestInfoOptional
    }

    suspend fun getPaymentInfoFromNodo(rptId: RptId, paymentContextCode: String): PaymentRequestInfo =
        withContext(dispatcher) {
            val paymentRequestInfo: PaymentRequestInfo
            logger.info(
                "Calling Nodo VerificaRPT for get payment info for rptId: [{}]. PaymentContextCode: [{}]",
                rptId.value,
                paymentContextCode
            )
            val nodoVerificaRPTRequest = baseNodoVerificaRPTRequest
            val nodoTipoCodiceIdRPT: NodoTipoCodiceIdRPT = nodoUtils.getCodiceIdRpt(rptId)
            nodoVerificaRPTRequest.codiceIdRPT = nodoTipoCodiceIdRPT
            nodoVerificaRPTRequest.codiceContestoPagamento = paymentContextCode
            val nodoVerificaRisposta =
                nodoPerPspClient.verificaRpt(objectFactoryNodoPerPsp.createNodoVerificaRPT(nodoVerificaRPTRequest))
                    .awaitSingle()
            val esitoNodoVerificaRPTRisposta = nodoVerificaRisposta.nodoVerificaRPTRisposta
            val faultBean = esitoNodoVerificaRPTRisposta.fault
            val isNM3 = isNM3(esitoNodoVerificaRPTRisposta)
            val isNodoError = isNodoError(esitoNodoVerificaRPTRisposta)
            logger.info(
                "Verifica RPT: fault code: [{}] isNM3: [{}], nodo error: [{}]",
                esitoNodoVerificaRPTRisposta?.fault?.faultCode,
                isNM3,
                isNodoError
            )
            if (isNodoError) {
                throw NodoErrorException(faultBean)
            }

            if (isNM3) {
                logger.info("Calling Nodo for VerifyPaymentNotice")
                val verifyPaymentNoticeReq = baseVerifyPaymentNoticeReq
                val qrCode = CtQrCode()
                qrCode.fiscalCode = rptId.fiscalCode
                qrCode.noticeNumber = rptId.noticeId
                verifyPaymentNoticeReq.qrCode = qrCode
                val verifyPaymentNoticeResponse = nodeForPspClient.verifyPaymentNotice(
                    objectFactoryNodeForPsp.createVerifyPaymentNoticeReq(verifyPaymentNoticeReq)
                ).awaitSingle()
                paymentRequestInfo = PaymentRequestInfo(
                    id = rptId,
                    paFiscalCode = verifyPaymentNoticeResponse.fiscalCodePA,
                    paName = verifyPaymentNoticeResponse.paymentDescription,
                    description = verifyPaymentNoticeResponse.paymentDescription,
                    amount = nodoOperations.getEuroCentsFromNodoAmount(
                        verifyPaymentNoticeResponse.paymentList.paymentOptionDescription[0].amount
                    ),
                    dueDate = getDueDateString(
                        verifyPaymentNoticeResponse.paymentList.paymentOptionDescription[0].dueDate
                    ),
                    isNM3 = true,
                    paymentToken = null,
                    idempotencyKey = null,
                    isCart = false
                )
            } else {
                val enteBeneficiario: CtEnteBeneficiario? =
                    esitoNodoVerificaRPTRisposta.datiPagamentoPA?.enteBeneficiario
                paymentRequestInfo =
                    PaymentRequestInfo(
                        id = rptId,
                        paFiscalCode = enteBeneficiario?.identificativoUnivocoBeneficiario?.codiceIdentificativoUnivoco,
                        paName = enteBeneficiario?.denominazioneBeneficiario,
                        description = esitoNodoVerificaRPTRisposta.datiPagamentoPA.causaleVersamento,
                        amount = nodoOperations.getEuroCentsFromNodoAmount(esitoNodoVerificaRPTRisposta.datiPagamentoPA.importoSingoloVersamento),
                        dueDate = null,
                        isNM3 = false,
                        paymentToken = null,
                        idempotencyKey = null,
                        isCart = false
                    )
            }
            return@withContext paymentRequestInfo
        }


    suspend fun isNM3(esitoNodoVerificaRPTRisposta: EsitoNodoVerificaRPTRisposta): Boolean {
        val outcome = esitoNodoVerificaRPTRisposta.esito
        val ko = StOutcome.KO.value().equals(outcome)
        return ko && (RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE == (esitoNodoVerificaRPTRisposta.fault?.faultCode))
    }

    suspend fun isNodoError(esitoNodoVerificaRPTRisposta: EsitoNodoVerificaRPTRisposta): Boolean {
        val outcome = esitoNodoVerificaRPTRisposta.esito
        val ko = StOutcome.KO.value().equals(outcome)
        return ko && (RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE != (esitoNodoVerificaRPTRisposta.fault?.faultCode))
    }

    suspend fun getDueDateString(date: XMLGregorianCalendar?): String? =
        date?.toString()
}