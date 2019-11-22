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
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.List;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_NEW_CARD_PROCESS;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE;

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
    private ClaimRepository claimRepository;
    private PaymentCycleService paymentCycleService;
    private MessageQueueClient messageQueueClient;
    private EventAuditor eventAuditor;
    private ClaimMessageSender claimMessageSender;

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
        updateClaim(claim, cardAccountId);
        PaymentCycle paymentCycle = createAndSavePaymentCycle(context);
        reportClaimUpdated(claim, cardAccountId, context.getEligibilityAndEntitlementDecision().getDateOfBirthOfChildren());
        sendMakeFirstPaymentMessage(paymentCycle);
        return COMPLETED;
    }

    private void updateClaim(Claim claim, String cardAccountId) {
        claim.setCardAccountId(cardAccountId);
        claim.updateClaimStatus(ACTIVE);
        claimRepository.save(claim);
    }

    private PaymentCycle createAndSavePaymentCycle(CompleteNewCardMessageContext context) {
        Claim claim = context.getClaim();
        return paymentCycleService.createAndSavePaymentCycleForEligibleClaim(
                claim,
                claim.getClaimStatusTimestamp().toLocalDate(),
                context.getEligibilityAndEntitlementDecision());
    }

    private void reportClaimUpdated(Claim claim, String cardAccountId, List<LocalDate> dateOfBirthOfChildren) {
        eventAuditor.auditNewCard(claim.getId(), cardAccountId);
        claimMessageSender.sendReportClaimMessage(claim, dateOfBirthOfChildren, UPDATED_FROM_NEW_TO_ACTIVE);
    }

    private void sendMakeFirstPaymentMessage(PaymentCycle paymentCycle) {
        MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MAKE_FIRST_PAYMENT);
    }
}
