package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.TextMessageContext;
import uk.gov.dhsc.htbhf.claimant.service.audit.FailedTextEvent;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendSmsResponse;

import java.util.UUID;

@Slf4j
@Component
public class TextMessageProcessor implements MessageTypeProcessor {

    private final NotificationClient client;
    private final MessageContextLoader messageContextLoader;

    public TextMessageProcessor(NotificationClient client,
                                MessageContextLoader messageContextLoader) {
        this.client = client;
        this.messageContextLoader = messageContextLoader;
    }

    @Override
    public MessageStatus processMessage(Message message) {

        TextMessageContext textMessageContext = messageContextLoader.loadTextMessageContext(message);
        UUID textReference = UUID.randomUUID();
        Claimant claimant = textMessageContext.getClaim().getClaimant();
        if (StringUtils.isNotEmpty(claimant.getPhoneNumber())) {
            try {
                SendSmsResponse sendSmsResponse = client.sendSms(
                        textMessageContext.getTextType().getTemplateId(),
                        textMessageContext.getClaim().getClaimant().getPhoneNumber(),
                        textMessageContext.getTextPersonalisation(),
                        textReference.toString()
                );

                log.debug("{} text sent, reference={}, id={}",textMessageContext.getTextType(), textReference, sendSmsResponse.getNotificationId());
            } catch (NotificationClientException e) {
                log.error("Failed to send text message", e);
                String failureMessage = String.format("Failed to send %s text message, exception is: %s", textMessageContext.getTextType(), e.getMessage());
                throw new EventFailedException(buildFailedTextEvent(textMessageContext), e, failureMessage);
            }
        }
        return MessageStatus.COMPLETED;
    }

    private FailedTextEvent buildFailedTextEvent(TextMessageContext context) {
        return FailedTextEvent.builder()
                .claimId(context.getClaim().getId())
                .textType(context.getTextType())
                .templateId(context.getTextType().getTemplateId())
                .build();
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.SEND_TEXT;
    }
}
