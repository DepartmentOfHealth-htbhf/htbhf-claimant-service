package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Message;

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

}
