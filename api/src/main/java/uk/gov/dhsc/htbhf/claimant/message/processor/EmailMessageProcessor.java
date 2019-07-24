package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.EmailMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.UUID;

@Slf4j
@Component
public class EmailMessageProcessor implements MessageTypeProcessor {

    private final NotificationClient client;
    private final MessageContextLoader messageContextLoader;
    private final String replyToAddressId;

    public EmailMessageProcessor(NotificationClient client,
                                 MessageContextLoader messageContextLoader,
                                 @Value("${notify.email-reply-to-id}") String replyToAddressId) {
        this.client = client;
        this.messageContextLoader = messageContextLoader;
        this.replyToAddressId = replyToAddressId;
    }

    @Override
    public MessageStatus processMessage(Message message) {
        EmailMessageContext messageContext = messageContextLoader.loadEmailMessageContext(message);
        UUID messageReference = UUID.randomUUID();
        try {
            SendEmailResponse sendEmailResponse = client.sendEmail(
                    messageContext.getTemplateId(),
                    messageContext.getClaim().getClaimant().getEmailAddress(),
                    messageContext.getEmailPersonalisation(),
                    messageReference.toString(),
                    replyToAddressId
            );
            log.debug("Email message sent, response: {}", sendEmailResponse);
        } catch (NotificationClientException e) {
            //TODO HTBHF-1885: Create a failed event if we catch an Exception and throw new FailedEventException.
            log.error("Failed to send email message", e);
        }
        return MessageStatus.COMPLETED;
    }

    @Override
    public MessageType supportsMessageType() {
        return MessageType.SEND_EMAIL;
    }
}
