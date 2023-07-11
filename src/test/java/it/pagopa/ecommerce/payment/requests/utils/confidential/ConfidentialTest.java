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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ConfidentialTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TokenApi personalDataVaultClient = Mockito.mock(TokenApi.class);

    private final ConfidentialDataManager confidentialDataManager = new ConfidentialDataManager(
            personalDataVaultClient
    );;

    @Test
    void confidentialJsonRepresentationIsOK() throws JsonProcessingException {
        Email email = new Email("foo@example.com");

        /* preconditions */
        Mockito.when(personalDataVaultClient.saveUsingPUT(any()))
                .thenReturn(Mono.just(new TokenResourceDto().token(UUID.randomUUID())));

        /* test */
        Confidential<Email> confidentialEmail = this.confidentialDataManager.encrypt(email)
                .block();

        String serialized = objectMapper.writeValueAsString(confidentialEmail);

        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        Map<String, Object> jsonData = objectMapper.readValue(serialized, typeRef);

        /* assertions */
        assertEquals(Set.of("data"), jsonData.keySet());
    }

    @Test
    void roundtripEncryptionDecryptionWithPDVIsSuccessful() throws JsonProcessingException {
        Email email = new Email("foo@example.com");

        TokenResourceDto emailToken = new TokenResourceDto().token(UUID.randomUUID());

        /* preconditions */
        Mockito.when(personalDataVaultClient.saveUsingPUT(new PiiResourceDto().pii(email.value())))
                .thenReturn(Mono.just(emailToken));

        Mockito.when(personalDataVaultClient.findPiiUsingGET(emailToken.getToken().toString()))
                .thenReturn(Mono.just(new PiiResourceDto().pii(email.value())));

        /* test */

        Confidential<Email> confidentialEmail = this.confidentialDataManager.encrypt(email)
                .block();

        String serialized = objectMapper.writeValueAsString(confidentialEmail);

        TypeReference<Confidential<Email>> typeRef = new TypeReference<>() {
        };
        Confidential<Email> deserialized = objectMapper.readValue(serialized, typeRef);

        Email decryptedEmail = confidentialDataManager.decrypt(deserialized, Email::new).block();

        /* assertions */
        assertEquals(email, decryptedEmail);
    }
}
