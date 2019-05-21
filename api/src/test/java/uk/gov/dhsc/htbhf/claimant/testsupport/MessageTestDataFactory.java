package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;

import java.time.LocalDateTime;

public class MessageTestDataFactory {

    public static final String MESSAGE_PAYLOAD = "{\"field1\":\"field1Value\"}";

    public static Message aValidMessage() {
        return buildValidMessageBuilder()
                .build();
    }

    public static Message aValidMessageWithType(MessageType type) {
        return buildValidMessageBuilder().messageType(type).build();
    }

    public static Message aValidMessageWithTypeAndTimestamp(MessageType type, LocalDateTime timestamp) {
        return buildValidMessageBuilder().messageType(type).messageTimestamp(timestamp).build();
    }

    public static Message aValidMessageWithTimestamp(LocalDateTime localDateTime) {
        return buildValidMessageBuilder().messageTimestamp(localDateTime).build();
    }

    public static Message aValidMessageWithPayload(String payload) {
        return buildValidMessageBuilder().messagePayload(payload).build();
    }

    private static Message.MessageBuilder buildValidMessageBuilder() {
        return Message.builder()
                .messageType(MessageType.CREATE_NEW_CARD)
                .messageTimestamp(LocalDateTime.now())
                .messagePayload(MESSAGE_PAYLOAD);
    }
}
