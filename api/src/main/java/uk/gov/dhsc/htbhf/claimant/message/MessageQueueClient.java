package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

/**
 * Interface for clients of the MessageQueue so that we can switch between implementations.
 */
public interface MessageQueueClient {

    /**
     * Send a message with the given payload of the given type.
     *
     * @param messagePayload The payload
     * @param messageType    The type of message
     */
    void sendMessage(MessagePayload messagePayload, MessageType messageType);
}
