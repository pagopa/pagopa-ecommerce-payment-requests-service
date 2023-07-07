package it.pagopa.ecommerce.payment.requests.utils.confidential.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager;

/**
 * <p>
 * An object holding confidential opaque data, along with the necessary metadata
 * to get the original data back.
 * </p>
 */
/*
 * @formatter:off
 *
 * Warning java:S2326 - T is not used in the record.
 * Suppressed because the whole point of this class is to mimic "as if"
 * an encrypted `T` was present and handle it transparently
 *
 * @formatter:on
 */
@SuppressWarnings("java:S2326")
public record Confidential<T extends ConfidentialDataManager.ConfidentialData> (
        @JsonProperty("data") String opaqueData
) {
    /**
     * Constructs a {@link Confidential} from existing data
     *
     * @param opaqueData opaque data (e.g. a ciphertext, a token to an external
     *                   service, etc.)
     */
    /*
     * @formatter:off
     *
     * Warning java:S1186 - Methods should not be empty
     * Warning java:S6207 - Redundant constructors/methods should be avoided in records
     * Both suppressed because this constructor is just to add the `@JsonCreator` annotation
     * and is currently the canonical way to add annotations to record constructors
     *
     * @formatter:on
     */
    @JsonCreator
    @SuppressWarnings(
        {
                "java:S1186",
                "java:S6207"
        }
    )
    public Confidential {
    }
}
