package it.pagopa.ecommerce.payment.requests.utils.soap.confidential;

import it.pagopa.ecommerce.payment.requests.utils.soap.confidential.domain.Confidential;
import it.pagopa.ecommerce.payment.requests.utils.soap.confidential.exceptions.ConfidentialDataException;
import it.pagopa.generated.pdv.v1.api.TokenApi;
import it.pagopa.generated.pdv.v1.dto.PiiResourceDto;
import it.pagopa.generated.pdv.v1.dto.TokenResourceDto;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Function;

/**
 * <p>
 * Class used to abstract over confidential data management and used to
 * construct and deconstruct {@link Confidential} instances.
 * </p>
 * <p>
 * This class is meant to be the entry point for an application to handle data
 * in and out of {@link Confidential} via the {@link #encrypt(ConfidentialData)}
 * and {@link #decrypt(Confidential)} methods
 * </p>
 */
public class ConfidentialDataManager {

    /**
     * <p>
     * Utility interface that handles conversions {@code T} -> {@link String}.
     * Classes that want to be put inside a {@link Confidential} need to implement
     * this interface, and provide a corresponding method to reconstruct instances
     * from a string.
     * </p>
     */
    public interface ConfidentialData {
        /**
         * A function to serialize the given data. Note that the implementor is not
         * bound to any serialization format as long as it can independently deserialize
         * it.
         *
         * @return serialized data.
         */
        @Nonnull
        String toStringRepresentation();
    }

    private final TokenApi personalDataVaultClient;

    /**
     * Primary constructor.
     *
     * @param personalDataVaultClient Client for Personal Data Vault
     */
    public ConfidentialDataManager(
            TokenApi personalDataVaultClient
    ) {
        this.personalDataVaultClient = personalDataVaultClient;
    }

    /**
     * <p>
     * Encrypts data with the given mode.
     * </p>
     * <br>
     * <p>
     * In case of error the returned {@link Mono} contains a
     * {@link ConfidentialDataException} wrapping one of the following exceptions:
     * <ul>
     * <li>InvalidAlgorithmParameterException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchPaddingException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>IllegalBlockSizeException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchAlgorithmException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>BadPaddingException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>InvalidKeyException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * </ul>
     *
     * @param data the unencrypted data
     * @return a {@link Confidential} instance containing the encrypted data and the
     *         algorithm metadata
     * @param <T> type of the unencrypted data
     */
    @Nonnull
    public <T extends ConfidentialData> Mono<Confidential<T>> encrypt(@Nonnull T data) {
        return encryptData(data.toStringRepresentation())
                .map(Confidential::new);
    }

    /**
     * <p>
     * Decrypts encrypted data.
     * </p>
     * <br>
     * <p>
     * In case of error the returned {@link Mono} contains a
     * {@link ConfidentialDataException} wrapping one of the following exceptions:
     * <ul>
     * <li>InvalidAlgorithmParameterException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchPaddingException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>IllegalBlockSizeException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchAlgorithmException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>BadPaddingException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>InvalidKeyException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * </ul>
     *
     * @param data        the data to be decrypted
     * @param constructor a function to construct {@code T} instances from the
     *                    serialized deciphered data
     * @return a {@code Mono<T>} instance built from the encrypted data
     * @param <T> the type of the object to be returned
     */
    @Nonnull
    public <T extends ConfidentialData> Mono<T> decrypt(
                                                        Confidential<T> data,
                                                        Function<String, T> constructor
    ) {
        return decrypt(data).map(constructor);
    }

    /**
     * <p>
     * Decrypts the data to string without conversion. Prefer
     * {@link ConfidentialDataManager#decrypt(Confidential, Function)} to this.
     * </p>
     * <br>
     * <p>
     * In case of error the returned {@link Mono} contains a
     * {@link ConfidentialDataException} wrapping one of the following exceptions:
     * <ul>
     * <li>InvalidAlgorithmParameterException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchPaddingException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>IllegalBlockSizeException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>NoSuchAlgorithmException See
     * {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>BadPaddingException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * <li>InvalidKeyException See {@link javax.crypto.Cipher#doFinal(byte[])}</li>
     * </ul>
     *
     * @param data the data to be decrypted
     * @return a {@code Mono<T>} instance built from the encrypted data
     * @param <T> the type of the object to be returned
     */
    @Nonnull
    public <T extends ConfidentialData> Mono<String> decrypt(Confidential<T> data) {
        return this.personalDataVaultClient.findPiiUsingGET(data.opaqueData())
                .map(PiiResourceDto::getPii)
                .onErrorMap(WebClientResponseException.class, ConfidentialDataException::new);
    }

    @Nonnull
    private Mono<String> encryptData(@Nonnull String data) {
        return this.personalDataVaultClient.saveUsingPUT(new PiiResourceDto().pii(data))
                .map(TokenResourceDto::getToken)
                .map(UUID::toString)
                .onErrorMap(WebClientResponseException.class, ConfidentialDataException::new);
    }
}
