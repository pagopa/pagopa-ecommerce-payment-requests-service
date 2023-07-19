package it.pagopa.ecommerce.payment.requests.utils.confidential.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {
    private static final String INVALID_EMAIL = "";
    private static final String VALID_EMAIL = "foo@example.com";

    private static final String UPPER_CASE_MAIL = "FOO@EXAMPLE.COM";

    private static final String MIXED_CASE_MAIL = "FoO@eXaMpLe.CoM";

    @ParameterizedTest
    @ValueSource(
            strings = {
                    VALID_EMAIL,
                    UPPER_CASE_MAIL,
                    MIXED_CASE_MAIL
            }
    )
    void shouldConstructValidEmail(String email) {
        assertDoesNotThrow(() -> new Email(email));
    }

    @Test
    void shouldThrowOnInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email(INVALID_EMAIL));
    }

}
