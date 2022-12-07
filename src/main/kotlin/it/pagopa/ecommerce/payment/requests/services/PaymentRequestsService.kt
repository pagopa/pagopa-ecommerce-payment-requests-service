package it.pagopa.ecommerce.payment.requests.services

import it.pagopa.ecommerce.generated.nodoperpsp.model.*
import it.pagopa.ecommerce.generated.payment.requests.server.model.PaymentRequestsGetResponseDto
import it.pagopa.ecommerce.generated.transactions.model.CtQrCode
import it.pagopa.ecommerce.generated.transactions.model.StOutcome
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeReq
import it.pagopa.ecommerce.generated.transactions.model.VerifyPaymentNoticeRes
import it.pagopa.ecommerce.payment.requests.client.NodeForPspClient
import it.pagopa.ecommerce.payment.requests.client.NodoPerPspClient
import it.pagopa.ecommerce.payment.requests.domain.RptId
import it.pagopa.ecommerce.payment.requests.exceptions.InvalidRptException
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfo
import it.pagopa.ecommerce.payment.requests.repositories.PaymentRequestInfoRepository
import it.pagopa.ecommerce.payment.requests.utils.NodoOperations
import it.pagopa.ecommerce.payment.requests.utils.NodoUtils
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
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
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE = "PPT_MULTI_BENEFICIARIO"
    }

    suspend fun getPaymentRequestInfo(rptId: String): PaymentRequestsGetResponseDto {
        val rptIdRecord: RptId
        try {
            rptIdRecord = RptId(rptId)
        } catch (e: IllegalArgumentException) {
            throw InvalidRptException(rptId)
        }
        val paymentContextCode = UUID.randomUUID().toString().replace("-", "")
        val paymentInfo = getPaymentInfoFromCache(rptIdRecord)
            .switchIfEmpty(
                Mono.defer {
                    getPaymentInfoFromNodo(rptIdRecord, paymentContextCode)
                        .doOnNext { paymentRequestFromNodo ->
                            logger.info(
                                "PaymentRequestInfo from nodo pagoPA for {}, isNM3 {}",
                                rptId,
                                paymentRequestFromNodo.isNM3
                            )
                        }.doOnSuccess { paymentRequestInfoRepository.save(it) }
                }
            ).map { paymentInfo ->
                PaymentRequestsGetResponseDto(
                    rptId = paymentInfo.id.value,
                    paFiscalCode = paymentInfo.paFiscalCode,
                    paName = paymentInfo.paName,
                    dueDate = paymentInfo.dueDate,
                    description = paymentInfo.description,
                    amount = paymentInfo.amount,
                    paymentContextCode = paymentContextCode
                )
            }.doOnNext { logger.info("PaymentRequestInfo retrieved for {}", rptId) }
        return paymentInfo.awaitSingle()
    }

    suspend fun getPaymentInfoFromCache(rptId: RptId): Mono<PaymentRequestInfo> {
        val paymentRequestInfoOptional: Optional<PaymentRequestInfo> = paymentRequestInfoRepository.findById(rptId)
        logger.info("PaymentRequestInfo cache hit for {}: {}", rptId, paymentRequestInfoOptional.isPresent)
        return paymentRequestInfoOptional.map { Mono.just(it) }.orElseGet { Mono.empty() }
    }

    fun getPaymentInfoFromNodo(rptId: RptId, paymentContextCode: String): Mono<PaymentRequestInfo> =
        Mono.just(rptId).flatMap { request: RptId ->
            logger.info(
                "Calling Nodo VerificaRPT for get payment info for rptId: [{}]. PaymentContextCode: [{}]",
                rptId.value,
                paymentContextCode
            )
            val nodoVerificaRPTRequest = baseNodoVerificaRPTRequest
            val nodoTipoCodiceIdRPT: NodoTipoCodiceIdRPT = nodoUtils.getCodiceIdRpt(request)
            nodoVerificaRPTRequest.codiceIdRPT = nodoTipoCodiceIdRPT
            nodoVerificaRPTRequest.codiceContestoPagamento = paymentContextCode
            nodoPerPspClient.verificaRpt(objectFactoryNodoPerPsp.createNodoVerificaRPT(nodoVerificaRPTRequest))
        }.flatMap {
            val esitoNodoVerificaRPTRisposta = it.nodoVerificaRPTRisposta
            val faultBean = esitoNodoVerificaRPTRisposta.fault
            val isNM3 = isNM3(esitoNodoVerificaRPTRisposta)
            val isNodoError = isNodoError(esitoNodoVerificaRPTRisposta)
            logger.info(
                "Verifica RPT: outcome: [{}] fault code: [{}] isNM3: [{}], is nodo error: [{}]",
                esitoNodoVerificaRPTRisposta.esito,
                esitoNodoVerificaRPTRisposta?.fault?.faultCode,
                isNM3,
                isNodoError
            )
            if (isNodoError) Mono.error(NodoErrorException(faultBean))
            else Mono.just(
                Tuples.of(
                    esitoNodoVerificaRPTRisposta,
                    isNM3
                )
            )
        }.flatMap {
            val esitoNodoVerificaRPTRisposta: EsitoNodoVerificaRPTRisposta = it.t1
            val isNM3: Boolean = it.t2
            val paymentRequestInfo: Mono<PaymentRequestInfo>
            if (isNM3) {
                logger.info("Calling Nodo for VerifyPaymentNotice")
                val verifyPaymentNoticeReq = baseVerifyPaymentNoticeReq
                val qrCode = CtQrCode()
                qrCode.fiscalCode = rptId.fiscalCode
                qrCode.noticeNumber = rptId.noticeId
                verifyPaymentNoticeReq.qrCode = qrCode
                paymentRequestInfo = nodeForPspClient.verifyPaymentNotice(
                    objectFactoryNodeForPsp.createVerifyPaymentNoticeReq(
                        verifyPaymentNoticeReq
                    )
                ).flatMap { verifyPaymentNoticeResponse ->
                    val isNodoError = isNodoError(verifyPaymentNoticeResponse)
                    logger.info(
                        "VerifyPaymentNotice: outcome: [{}] fault code: [{}] is nodo error: [{}]",
                        verifyPaymentNoticeResponse.outcome,
                        verifyPaymentNoticeResponse?.fault?.faultCode,
                        isNodoError
                    )
                    if (isNodoError) {
                        Mono.error(NodoErrorException(verifyPaymentNoticeResponse.fault))
                    } else {
                        Mono.just(
                            PaymentRequestInfo(
                                id = rptId,
                                paFiscalCode = verifyPaymentNoticeResponse.fiscalCodePA,
                                paName = verifyPaymentNoticeResponse.paName,
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
                        )
                    }
                }
            } else {
                val enteBeneficiario: CtEnteBeneficiario? =
                    esitoNodoVerificaRPTRisposta.datiPagamentoPA?.enteBeneficiario
                paymentRequestInfo = Mono.just(
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
                )
            }
            return@flatMap paymentRequestInfo
        }


    fun isNM3(esitoNodoVerificaRPTRisposta: EsitoNodoVerificaRPTRisposta): Boolean {
        val outcome = esitoNodoVerificaRPTRisposta.esito
        val ko = StOutcome.KO.value().equals(outcome)
        return ko && (RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE == (esitoNodoVerificaRPTRisposta.fault?.faultCode))
    }

    fun isNodoError(esitoNodoVerificaRPTRisposta: EsitoNodoVerificaRPTRisposta): Boolean {
        val outcome = esitoNodoVerificaRPTRisposta.esito
        val ko = StOutcome.KO.value().equals(outcome)
        return ko && (RPT_VERIFY_MULTI_BENEFICIARY_RESPONSE_CODE != (esitoNodoVerificaRPTRisposta.fault?.faultCode))
    }

    fun isNodoError(verifyPaymentResponse: VerifyPaymentNoticeRes): Boolean {
        val outcome = verifyPaymentResponse.outcome
        return StOutcome.KO == outcome
    }

    fun getDueDateString(date: XMLGregorianCalendar?): String? =
        date?.toString()
}