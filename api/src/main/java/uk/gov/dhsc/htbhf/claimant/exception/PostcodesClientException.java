package uk.gov.dhsc.htbhf.claimant.exception;

/**
 * Exception used when calling postcodes.io.
 */
public class PostcodesClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PostcodesClientException(String message) {
        super(message);
    }

    public PostcodesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
