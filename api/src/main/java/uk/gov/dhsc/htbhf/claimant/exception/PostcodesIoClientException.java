package uk.gov.dhsc.htbhf.claimant.exception;

/**
 * Exception used when calling postcodes.io.
 */
public class PostcodesIoClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PostcodesIoClientException(String message) {
        super(message);
    }

    public PostcodesIoClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
