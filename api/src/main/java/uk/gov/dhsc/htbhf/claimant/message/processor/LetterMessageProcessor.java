package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.LetterMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.service.audit.FailedLetterEvent;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendLetterResponse;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LetterMessageProcessor implements MessageTypeProcessor {

    private final NotificationClient client;
    private final MessageContextLoader messageContextLoader;

    @Override
    public MessageStatus processMessage(Message message) {
        LetterMessageContext messageContext = messageContextLoader.loadLetterMessageContext(message);
        String messageReference = UUID.randomUUID().toString();
        try {
            SendLetterResponse sendLetterResponse = client.sendLetter(
                    messageContext.getLetterType().getTemplateId(),
                    messageContext.getPersonalisation(),
                    messageReference
            );
            log.debug("{} letter sent, reference={}, id={}", messageContext.getLetterType(), messageReference, sendLetterResponse.getNotificationId());
        } catch (NotificationClientException e) {
            log.error("Failed to send letter message", e);
            String failureMessage = String.format("Failed to send %s letter, exception is: %s", messageContext.getLetterType(), e.getMessage());
            throw new EventFailedException(buildFailedLetterEvent(messageContext), e, failureMessage);
        }
        return MessageStatus.COMPLETED;
    }

    private FailedLetterEvent buildFailedLetterEvent(LetterMessageContext context) {
        return FailedLetterEvent.builder()
                .claimId(context.getClaim().getId())
                .letterType(context.getLetterType())
                .templateId(context.getLetterType().getTemplateId())
                .build();
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.SEND_LETTER;
    }
}
