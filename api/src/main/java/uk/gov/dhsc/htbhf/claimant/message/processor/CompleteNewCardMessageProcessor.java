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
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.message.payload.RequestPaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimActivationService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildRequestPaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_NEW_CARD_PROCESS;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_PAYMENT;

/**
 * Responsible for processing {@link MessageType#COMPLETE_NEW_CARD_PROCESS} messages by:
 * Saving the card account id to the claim,
 * Creating a PaymentCycle for the claim,
 * Sending a {@link MessageType#REQUEST_PAYMENT} message.
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
        RequestPaymentMessagePayload messagePayload = buildRequestPaymentMessagePayload(paymentCycle, PaymentType.FIRST_PAYMENT);
        messageQueueClient.sendMessage(messagePayload, REQUEST_PAYMENT);
    }
}
