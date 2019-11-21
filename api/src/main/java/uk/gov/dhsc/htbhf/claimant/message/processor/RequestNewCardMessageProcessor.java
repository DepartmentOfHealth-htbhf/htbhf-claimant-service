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
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.RequestNewCardService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;

/**
 * Responsible for processing {@link MessageType#REQUEST_NEW_CARD} messages by:
 * Creating a new card,
 * Creating a PaymentCycle for the claim,
 * Sending a MAKE_FIRST_PAYMENT message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class RequestNewCardMessageProcessor implements MessageTypeProcessor {

    private RequestNewCardService requestNewCardService;
    private MessageContextLoader messageContextLoader;
    private PaymentCycleService paymentCycleService;
    private MessageQueueClient messageQueueClient;

    @Override
    public MessageType supportsMessageType() {
        return REQUEST_NEW_CARD;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        RequestNewCardMessageContext context = messageContextLoader.loadRequestNewCardContext(message);
        EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision = context.getEligibilityAndEntitlementDecision();
        requestNewCardService.createNewCard(context.getClaim(), eligibilityAndEntitlementDecision.getDateOfBirthOfChildren());
        PaymentCycle paymentCycle = createAndSavePaymentCycle(context);
        sendMakeFirstPaymentMessage(paymentCycle);
        return COMPLETED;
    }

    private PaymentCycle createAndSavePaymentCycle(RequestNewCardMessageContext context) {
        Claim claim = context.getClaim();
        return paymentCycleService.createAndSavePaymentCycleForEligibleClaim(
                claim,
                claim.getClaimStatusTimestamp().toLocalDate(),
                context.getEligibilityAndEntitlementDecision());
    }

    private void sendMakeFirstPaymentMessage(PaymentCycle paymentCycle) {
        MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MAKE_FIRST_PAYMENT);
    }

}
