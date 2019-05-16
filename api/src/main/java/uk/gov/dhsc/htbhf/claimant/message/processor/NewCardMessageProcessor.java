package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.NewCardMessageContext;
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

    private MessageContextLoader messageContextLoader;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        NewCardMessageContext context = messageContextLoader.loadNewCardContext(message);
        newCardService.createNewCard(context.getClaim());
        messageRepository.delete(message);
        return COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return CREATE_NEW_CARD;
    }

}
