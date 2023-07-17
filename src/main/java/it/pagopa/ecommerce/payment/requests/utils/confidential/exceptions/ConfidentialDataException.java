package it.pagopa.ecommerce.payment.requests.utils.confidential.exceptions;

import it.pagopa.ecommerce.payment.requests.utils.confidential.ConfidentialDataManager;

/**
 * Exception class wrapping checked exceptions that can occur during encryption
 * or decryption of confidential data
 *
 * @see ConfidentialDataManager
 */
public class ConfidentialDataException extends RuntimeException {

    /**
     * Primary exception constructor
     *
     * @param e the exception to be wrapped
     */
    public ConfidentialDataException(Exception e) {
        super("Exception during confidential data encrypt/decrypt", e);
    }
}
