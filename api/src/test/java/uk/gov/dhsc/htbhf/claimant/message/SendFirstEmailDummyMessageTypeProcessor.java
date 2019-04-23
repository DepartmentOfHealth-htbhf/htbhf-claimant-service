package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Message;

import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_FIRST_EMAIL;

public class SendFirstEmailDummyMessageTypeProcessor implements MessageTypeProcessor {

    @Override
    public MessageStatus processMessage(Message message) {
        return null;
    }

    @Override
    public MessageType supportsMessageType() {
        return SEND_FIRST_EMAIL;
    }
}
