package it.pagopa.ecommerce.payment.requests.utils.client;

import it.pagopa.ecommerce.payment.requests.exceptions.RestApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResponseSpecCustomTest {

    @Mock
    private ResponseSpecCustom responseSpecCustom;

    @Test
    void shouldReturnException() {
        when(responseSpecCustom.getStatus()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        when(responseSpecCustom.onStatus(any(), any())).thenCallRealMethod();
        assertThrows(
                RestApiException.class,
                () -> responseSpecCustom.onStatus(
                        HttpStatus::isError,
                        clientResponse -> Mono.error(
                                new RestApiException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "Error title",
                                        "Error message"
                                )
                        )
                )

        );
    }

    @Test
    void shouldReturnNoError() {
        when(responseSpecCustom.getStatus()).thenReturn(HttpStatus.OK);
        when(responseSpecCustom.onStatus(any(), any())).thenCallRealMethod();
        assertDoesNotThrow(
                () -> responseSpecCustom.onStatus(
                        HttpStatus::isError,
                        clientResponse -> Mono.error(
                                new RestApiException(
                                        HttpStatus.UNPROCESSABLE_ENTITY,
                                        "Error title",
                                        "Error message"
                                )
                        )
                )

        );
    }
}
