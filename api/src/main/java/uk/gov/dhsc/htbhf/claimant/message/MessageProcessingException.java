package uk.gov.dhsc.htbhf.claimant.message;

/**
 * Runtime exception used when problems are found processing messages.
 */
public class MessageProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MessageProcessingException(String message) {
        super(message);
    }

    public MessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
