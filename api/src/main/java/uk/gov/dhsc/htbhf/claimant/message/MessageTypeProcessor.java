package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

/**
 * Interface to be used by an processor for a single type of message.
 */
public interface MessageTypeProcessor {

    /**
     * To be implemented by each processor to process a single message.
     *
     * @param message The message to process.
     * @return The status of the message after processing
     */
    MessageStatus processMessage(Message message);

    /**
     * The enum representing the type of message this processor supports.
     *
     * @return The {@link MessageType}
     */
    MessageType supportsMessageType();

    /**
     * Can be implemented by a MessageTypeProcessor to define custom functionality that needs to be performed
     * when the processing of a message has failed. Default functionality as defined here is to do nothing.
     *
     * @param message      The message that has failed
     * @param failureEvent The details of the failed event.
     */
    default void processFailedMessage(Message message, FailureEvent failureEvent) {
        // No action required
    }

}
