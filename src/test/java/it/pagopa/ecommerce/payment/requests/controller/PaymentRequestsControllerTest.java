package it.pagopa.ecommerce.payment.requests.controller;

import it.pagopa.ecommerce.generated.payment.requests.server.model.*;
import it.pagopa.ecommerce.payment.requests.controllers.PaymentRequestsController;
import it.pagopa.ecommerce.payment.requests.errorhandling.ExceptionHandler;
import it.pagopa.ecommerce.payment.requests.exceptions.NodoErrorException;
import it.pagopa.ecommerce.payment.requests.services.PaymentRequestsService;
import it.pagopa.generated.nodoperpsp.model.FaultBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRequestsControllerTest {

    @InjectMocks
    private PaymentRequestsController paymentRequestsController;

    @Mock
    private PaymentRequestsService paymentRequestsService;


    private static FaultBean faultBeanWithCode(String faultCode) {
        FaultBean fault = new FaultBean();
        fault.setFaultCode(faultCode);

        return fault;
    }

    @Test
    void shouldGetPaymentInfoGivenValidRptid() {
        String RPTID = "77777777777302016723749670035";

        PaymentRequestsGetResponseDto response = new PaymentRequestsGetResponseDto(
                1000,
                null,
                RPTID,
                null,
                null,
                "Payment test",
                null
        );
        when(paymentRequestsService.getPaymentRequestInfo(RPTID, null)).thenReturn(Mono.just(response));

        ResponseEntity<PaymentRequestsGetResponseDto> responseEntity =
                (ResponseEntity<PaymentRequestsGetResponseDto>) paymentRequestsController.getPaymentRequestInfo(RPTID, null);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void shouldGenericBadGatewayResponse()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "genericBadGatewayHandler", RuntimeException.class);
        method.setAccessible(true);

        ResponseEntity<ProblemJsonDto> responseEntity =
                (ResponseEntity<ProblemJsonDto>)
                        method.invoke(paymentRequestsController, new RuntimeException());

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
    }

    @Test
    void shouldReturnResponseEntityWithPartyConfigurationFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FaultBean faultBean = faultBeanWithCode(PartyConfigurationFaultDto.pPTDOMINIODISABILITATO.getValue());

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<PartyConfigurationFaultPaymentProblemJsonDto> responseEntity =
                (ResponseEntity<PartyConfigurationFaultPaymentProblemJsonDto>)
                        method.invoke(
                                paymentRequestsController,
                                new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertEquals(
                FaultCategoryDto.pAYMENTUNAVAILABLE, responseEntity.getBody().getFaultCodeCategory());
        assertEquals(
                PartyConfigurationFaultDto.pPTDOMINIODISABILITATO.getValue(),
                responseEntity.getBody().getFaultCodeDetail().getValue());
    }

    @Test
    void shouldReturnResponseEntityWithValidationFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FaultBean faultBean = faultBeanWithCode(ValidationFaultDto.pPTDOMINIOSCONOSCIUTO.getValue());

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<ValidationFaultPaymentProblemJsonDto> responseEntity =
                (ResponseEntity<ValidationFaultPaymentProblemJsonDto>)
                        method.invoke(
                                paymentRequestsController,
                                new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(FaultCategoryDto.pAYMENTUNKNOWN, responseEntity.getBody().getFaultCodeCategory());
        assertEquals(
                ValidationFaultDto.pPTDOMINIOSCONOSCIUTO.getValue(),
                responseEntity.getBody().getFaultCodeDetail().getValue());
    }

    @Test
    void shouldReturnResponseEntityWithGatewayFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FaultBean faultBean = faultBeanWithCode(GatewayFaultDto.pAASYSTEMERROR.getValue());

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<GatewayFaultPaymentProblemJsonDto> responseEntity =
                (ResponseEntity<GatewayFaultPaymentProblemJsonDto>)
                        method.invoke(
                                paymentRequestsController,
                                new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertEquals(FaultCategoryDto.gENERICERROR, responseEntity.getBody().getFaultCodeCategory());
        assertEquals(
                GatewayFaultDto.pAASYSTEMERROR.getValue(),
                responseEntity.getBody().getFaultCodeDetail().getValue());
    }

    @Test
    void shouldReturnResponseEntityWithPartyTimeoutFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        FaultBean faultBean = faultBeanWithCode(PartyTimeoutFaultDto.pPTSTAZIONEINTPAIRRAGGIUNGIBILE.getValue());

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<PartyTimeoutFaultPaymentProblemJsonDto> responseEntity =
                (ResponseEntity<PartyTimeoutFaultPaymentProblemJsonDto>)
                        method.invoke(
                                paymentRequestsController,
                                new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, responseEntity.getStatusCode());
        assertEquals(FaultCategoryDto.gENERICERROR, responseEntity.getBody().getFaultCodeCategory());
        assertEquals(
                PartyTimeoutFaultDto.pPTSTAZIONEINTPAIRRAGGIUNGIBILE.getValue(),
                responseEntity.getBody().getFaultCodeDetail().getValue());
    }

    @Test
    void shouldReturnResponseEntityWithPaymentStatusFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FaultBean faultBean = faultBeanWithCode(PaymentStatusFaultDto.pAAPAGAMENTOINCORSO.getValue());

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<PaymentStatusFaultPaymentProblemJsonDto> responseEntity =
                (ResponseEntity<PaymentStatusFaultPaymentProblemJsonDto>)
                        method.invoke(
                                paymentRequestsController,
                                new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        assertEquals(
                FaultCategoryDto.pAYMENTUNAVAILABLE, responseEntity.getBody().getFaultCodeCategory());
        assertEquals(
                PaymentStatusFaultDto.pAAPAGAMENTOINCORSO.getValue(),
                responseEntity.getBody().getFaultCodeDetail().getValue());
    }

    @Test
    void shouldReturnResponseEntityWithGenericGatewayFault()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FaultBean faultBean = faultBeanWithCode("UNKNOWN_ERROR");

        Method method =
                ExceptionHandler.class.getDeclaredMethod(
                        "nodoErrorHandler", NodoErrorException.class);
        method.setAccessible(true);

        ResponseEntity<ProblemJsonDto> responseEntity =
                (ResponseEntity<ProblemJsonDto>)
                        method.invoke(paymentRequestsController, new NodoErrorException(faultBean));

        assertEquals(Boolean.TRUE, responseEntity != null);
        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
    }
}
