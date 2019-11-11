package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.*;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.NewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;

/**
 * Responsible for processing CREATE_NEW_CARD messages by:
 * Creating a new card,
 * Creating a PaymentCycle for the claim,
 * Sending a MAKE_FIRST_PAYMENT message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class NewCardMessageProcessor implements MessageTypeProcessor {

    private NewCardService newCardService;
    private MessageContextLoader messageContextLoader;
    private PaymentCycleService paymentCycleService;
    private MessageQueueClient messageQueueClient;
    private EmailMessagePayloadFactory emailMessagePayloadFactory;

    @Override
    public MessageType supportsMessageType() {
        return CREATE_NEW_CARD;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        NewCardMessageContext context = messageContextLoader.loadNewCardContext(message);
        newCardService.createNewCard(context.getClaim(), context.getDatesOfBirthOfChildren());
        PaymentCycle paymentCycle = createAndSavePaymentCycle(context);
        sendMakeFirstPaymentMessage(paymentCycle);
        sendNewCardSuccessEmailMessage(paymentCycle);
        return COMPLETED;
    }

    private PaymentCycle createAndSavePaymentCycle(NewCardMessageContext context) {
        Claim claim = context.getClaim();
        return paymentCycleService.createAndSavePaymentCycleForEligibleClaim(
                claim,
                claim.getClaimStatusTimestamp().toLocalDate(),
                context.getPaymentCycleVoucherEntitlement(),
                context.getDatesOfBirthOfChildren());
    }

    private void sendMakeFirstPaymentMessage(PaymentCycle paymentCycle) {
        MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MAKE_FIRST_PAYMENT);
    }

    private void sendNewCardSuccessEmailMessage(PaymentCycle paymentCycle) {
        EmailMessagePayload messagePayload = emailMessagePayloadFactory.buildEmailMessagePayload(paymentCycle, EmailType.INSTANT_SUCCESS);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }

}
