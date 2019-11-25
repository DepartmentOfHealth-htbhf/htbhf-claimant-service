package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.CompleteNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimActivationService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_NEW_CARD_PROCESS;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;

/**
 * Responsible for processing {@link MessageType#COMPLETE_NEW_CARD_PROCESS} messages by:
 * Saving the card account id to the claim,
 * Creating a PaymentCycle for the claim,
 * Sending a {@link MessageType#MAKE_FIRST_PAYMENT} message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class CompleteNewCardMessageProcessor implements MessageTypeProcessor {

    private MessageContextLoader messageContextLoader;
    private ClaimActivationService claimActivationService;
    private MessageQueueClient messageQueueClient;

    @Override
    public MessageType supportsMessageType() {
        return COMPLETE_NEW_CARD_PROCESS;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        CompleteNewCardMessageContext context = messageContextLoader.loadCompleteNewCardContext(message);
        Claim claim = context.getClaim();
        String cardAccountId = context.getCardAccountId();
        EligibilityAndEntitlementDecision decision = context.getEligibilityAndEntitlementDecision();
        PaymentCycle paymentCycle = claimActivationService.updateClaimAndCreatePaymentCycle(claim, cardAccountId, decision);
        sendMakeFirstPaymentMessage(paymentCycle);
        return COMPLETED;
    }

    private void sendMakeFirstPaymentMessage(PaymentCycle paymentCycle) {
        MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MAKE_FIRST_PAYMENT);
    }
}
