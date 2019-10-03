package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.ClaimEmailHandler;
import uk.gov.dhsc.htbhf.claimant.entity.*;
import uk.gov.dhsc.htbhf.claimant.message.*;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@Slf4j
@Component
@AllArgsConstructor
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private EligibilityAndEntitlementService eligibilityAndEntitlementService;

    private MessageContextLoader messageContextLoader;

    private PaymentCycleService paymentCycleService;

    private ClaimRepository claimRepository;

    private MessageQueueClient messageQueueClient;

    private ClaimEmailHandler claimEmailHandler;

    private EventAuditor eventAuditor;

    @Override
    public MessageType supportsMessageType() {
        return DETERMINE_ENTITLEMENT;
    }

    /**
     * Processes DETERMINE_ENTITLEMENT messages from the message queue by determining the eligibility and entitlement of the
     * claimant for the current Payment Cycle. The entitlement and eligibility are then persisted to the current Payment Cycle.
     *
     * @param message The message to process.
     * @return The message status on completion
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        DetermineEntitlementMessageContext messageContext = messageContextLoader.loadDetermineEntitlementContext(message);
        Claim claim = messageContext.getClaim();
        Claimant claimant = claim.getClaimant();
        PaymentCycle currentPaymentCycle = messageContext.getCurrentPaymentCycle();
        PaymentCycle previousPaymentCycle = messageContext.getPreviousPaymentCycle();

        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateExistingClaimant(
                claimant,
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle);

        paymentCycleService.updatePaymentCycle(currentPaymentCycle, decision);

        if (decision.getVoucherEntitlement().getTotalVoucherValueInPence() == 0) {
            setClaimToExpired(claim);
            eventAuditor.auditClaimExpired(claim);
        } else if (decision.getEligibilityStatus() == ELIGIBLE) {
            createMakePaymentMessage(currentPaymentCycle);
        } else {
            handleNonEligibleClaim(currentPaymentCycle);
        }

        return COMPLETED;
    }

    private void setClaimToExpired(Claim claim) {
        claim.setClaimStatus(ClaimStatus.EXPIRED);
        claimRepository.save(claim);
    }

    private void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    private void handleNonEligibleClaim(PaymentCycle currentPaymentCycle) {
        Claim claim = currentPaymentCycle.getClaim();
        claim.setClaimStatus(ClaimStatus.PENDING_EXPIRY);
        claimRepository.save(claim);
        claimEmailHandler.sendClaimNoLongerEligibleEmail(claim);
    }

}
