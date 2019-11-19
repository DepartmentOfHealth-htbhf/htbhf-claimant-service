package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Message;

import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;

public class CreateNewCardDummyMessageTypeProcessor implements MessageTypeProcessor {

    @Override
    public MessageStatus processMessage(Message message) {
        return MessageStatus.COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return REQUEST_NEW_CARD;
    }
}
