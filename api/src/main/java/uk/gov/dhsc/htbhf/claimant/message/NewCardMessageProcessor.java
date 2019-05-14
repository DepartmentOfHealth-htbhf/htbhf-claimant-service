package uk.gov.dhsc.htbhf.claimant.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;

@Component
@AllArgsConstructor
@Slf4j
public class NewCardMessageProcessor implements MessageTypeProcessor {

    private NewCardService newCardService;

    private MessageRepository messageRepository;

    private PayloadMapper payloadMapper;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        NewCardRequestMessagePayload payload = payloadMapper.getPayload(message, NewCardRequestMessagePayload.class);
        newCardService.createNewCard(payload.getClaimId());
        messageRepository.delete(message);
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return CREATE_NEW_CARD;
    }

}
