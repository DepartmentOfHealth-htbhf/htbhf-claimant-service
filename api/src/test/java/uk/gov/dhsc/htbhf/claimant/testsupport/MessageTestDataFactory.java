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
        return buildValidMessageBuilder().messageType(type).processAfter(timestamp).build();
    }

    public static Message aMessageWithCreatedTimestamp(LocalDateTime localDateTime) {
        return buildValidMessageBuilder().createdTimestamp(localDateTime).build();
    }

    public static Message aMessageWithMessageTimestamp(LocalDateTime localDateTime) {
        return buildValidMessageBuilder().processAfter(localDateTime).build();
    }

    public static Message aMessageWithMessageTimestampAndDeliveryCount(LocalDateTime localDateTime, int deliveryCount) {
        return buildValidMessageBuilder()
                .processAfter(localDateTime)
                .deliveryCount(deliveryCount)
                .build();
    }

    public static Message aValidMessageWithPayload(String payload) {
        return buildValidMessageBuilder().messagePayload(payload).build();
    }

    private static Message.MessageBuilder buildValidMessageBuilder() {
        return Message.builder()
                .createdTimestamp(LocalDateTime.now())
                .messageType(MessageType.CREATE_NEW_CARD)
                .processAfter(LocalDateTime.now())
                .messagePayload(MESSAGE_PAYLOAD);
    }
}
