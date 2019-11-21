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
import uk.gov.dhsc.htbhf.claimant.message.context.SaveNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory.buildMakePaymentMessagePayload;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SAVE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_NEW_TO_ACTIVE;

/**
 * Responsible for processing {@link MessageType#SAVE_NEW_CARD} messages by:
 * Saving the card account id to the claim,
 * Creating a PaymentCycle for the claim,
 * Sending a {@link MessageType#MAKE_FIRST_PAYMENT} message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class SaveNewCardMessageProcessor implements MessageTypeProcessor {

    private MessageContextLoader messageContextLoader;
    private ClaimRepository claimRepository;
    private PaymentCycleService paymentCycleService;
    private MessageQueueClient messageQueueClient;
    private EventAuditor eventAuditor;
    private ClaimMessageSender claimMessageSender;

    @Override
    public MessageType supportsMessageType() {
        return SAVE_NEW_CARD;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        SaveNewCardMessageContext context = messageContextLoader.loadSaveNewCardContext(message);
        saveCardAccountIdToClaim(context);
        eventAuditor.auditNewCard(context.getClaim().getId(), context.getCardAccountId());
        claimMessageSender.sendReportClaimMessage(context.getClaim(), context.getEligibilityAndEntitlementDecision().getDateOfBirthOfChildren(),
                UPDATED_FROM_NEW_TO_ACTIVE);
        PaymentCycle paymentCycle = createAndSavePaymentCycle(context);
        sendMakeFirstPaymentMessage(paymentCycle);
        return COMPLETED;
    }

    private PaymentCycle createAndSavePaymentCycle(SaveNewCardMessageContext context) {
        Claim claim = context.getClaim();
        return paymentCycleService.createAndSavePaymentCycleForEligibleClaim(
                claim,
                claim.getClaimStatusTimestamp().toLocalDate(),
                context.getEligibilityAndEntitlementDecision());
    }

    private void saveCardAccountIdToClaim(SaveNewCardMessageContext context) {
        Claim claim = context.getClaim();
        claim.setCardAccountId(context.getCardAccountId());
        claim.updateClaimStatus(ACTIVE);
        claimRepository.save(claim);
    }

    private void sendMakeFirstPaymentMessage(PaymentCycle paymentCycle) {
        MakePaymentMessagePayload messagePayload = buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MAKE_FIRST_PAYMENT);
    }
}
