package uk.gov.dhsc.htbhf.claimant.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.io.IOException;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;

@Component
@AllArgsConstructor
@Slf4j
public class NewCardMessageProcessor implements MessageTypeProcessor {

    private NewCardService newCardService;

    private ObjectMapper objectMapper;

    private MessageRepository messageRepository;

    //TODO MRS 2019-04-24: Unit test
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        try {
            NewCardRequestMessagePayload payload = objectMapper.readValue(message.getMessagePayload(), NewCardRequestMessagePayload.class);
            newCardService.createNewCard(payload.getClaimId());
            messageRepository.delete(message);
            return MessageStatus.COMPLETED;
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Unable to create message payload for message with id: {}, payload is: {}", message.getId(), message.getMessagePayload(), e);
            }
            return MessageStatus.FAILED;
        }
    }

    @Override
    public MessageType supportsMessageType() {
        return CREATE_NEW_CARD;
    }

}
