package it.pagopa.ecommerce.payment.requests.utils.confidential.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * <p>
 * A value object holding a valid email address.
 * </p>
 * <p>
 * Email addresses are validated against an RFC 5332 compliant regular
 * expression.
 * </p>
 *
 * @param value email address
 */
public record Email(String value)
        implements
        ConfidentialDataManager.ConfidentialData {
    private static final Pattern emailRegex = Pattern.compile(
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])" // NOSONAR
    );

    /**
     * {@link Email} constructor. Validates the input email.
     *
     * @param value email address
     * @throws IllegalArgumentException if the email is not valid
     */
    public Email {
        if (!emailRegex.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Ill-formed email: " + value + ". Doesn't match format: " + emailRegex.pattern()
            );
        }
    }

    @Nonnull
    @JsonValue
    @Override
    public String toStringRepresentation() {
        return value;
    }
}
