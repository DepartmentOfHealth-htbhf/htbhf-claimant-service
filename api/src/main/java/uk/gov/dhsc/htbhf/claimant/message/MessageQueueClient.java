package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.time.Duration;

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

    /**
     * Send a message with the given payload of the given type with a given delay.
     *
     * @param messagePayload The payload
     * @param messageType    The type of message
     * @param messageDelay   period of time to delay processing the message by
     */
    void sendMessageWithDelay(MessagePayload messagePayload, MessageType messageType, Duration messageDelay);
}
