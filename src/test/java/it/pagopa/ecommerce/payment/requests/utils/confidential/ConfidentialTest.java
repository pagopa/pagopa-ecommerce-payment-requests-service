package it.pagopa.ecommerce.payment.requests.utils.confidential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Confidential;
import it.pagopa.ecommerce.payment.requests.utils.confidential.domain.Email;
import it.pagopa.generated.pdv.v1.api.TokenApi;
import it.pagopa.generated.pdv.v1.dto.PiiResourceDto;
import it.pagopa.generated.pdv.v1.dto.TokenResourceDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConfidentialTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TokenApi personalDataVaultClient = Mockito.mock(TokenApi.class);

    private final ConfidentialDataManager confidentialDataManager = new ConfidentialDataManager(
            personalDataVaultClient
    );;

    @Test
    void confidentialJsonRepresentationIsOK() {
        Email email = new Email("foo@example.com");
        /* preconditions */
        given(personalDataVaultClient.saveUsingPUT(any()))
                .willReturn(Mono.just(new TokenResourceDto().token(UUID.randomUUID())));
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        /* test */
        StepVerifier.create(confidentialDataManager.encrypt(email))
                .consumeNextWith(
                        next -> {
                            try {
                                assertEquals(
                                        Set.of("data"),
                                        objectMapper.readValue(objectMapper.writeValueAsString(next), typeRef).keySet()
                                );
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).verifyComplete();
    }

    @Test
    void roundtripEncryptionDecryptionWithPDVIsSuccessful() throws JsonProcessingException {
        Email email = new Email("foo@example.com");

        TokenResourceDto emailToken = new TokenResourceDto().token(UUID.randomUUID());

        /* preconditions */
        given(personalDataVaultClient.saveUsingPUT(new PiiResourceDto().pii(email.value())))
                .willReturn(Mono.just(emailToken));

        given(personalDataVaultClient.findPiiUsingGET(emailToken.getToken().toString()))
                .willReturn(Mono.just(new PiiResourceDto().pii(email.value())));

        /* test */

        StepVerifier.create(confidentialDataManager.encrypt(email))
                .consumeNextWith(
                        next -> {
                            assertEquals(next.opaqueData(), emailToken.getToken().toString());
                        }
                ).verifyComplete();

        StepVerifier.create(confidentialDataManager.decrypt(new Confidential<>(emailToken.getToken().toString())))
                .consumeNextWith(
                        next -> {
                            assertEquals(next, email.value());
                        }
                ).verifyComplete();

    }
}
