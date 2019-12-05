package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.eligibility.EligibilityDecisionHandler;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDateTime;
import java.time.Period;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@Slf4j
@Component
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private final Period maximumPendingExpiryDuration;
    private final EligibilityAndEntitlementService eligibilityAndEntitlementService;
    private final MessageContextLoader messageContextLoader;
    private final PaymentCycleService paymentCycleService;
    private final EligibilityDecisionHandler eligibilityDecisionHandler;

    public DetermineEntitlementMessageProcessor(
            @Value("${payment-cycle.maximum-pending-expiry-duration}") Period maximumPendingExpiryDuration,
            EligibilityAndEntitlementService eligibilityAndEntitlementService,
            MessageContextLoader messageContextLoader,
            PaymentCycleService paymentCycleService,
            EligibilityDecisionHandler eligibilityDecisionHandler) {
        this.maximumPendingExpiryDuration = maximumPendingExpiryDuration;
        this.eligibilityAndEntitlementService = eligibilityAndEntitlementService;
        this.messageContextLoader = messageContextLoader;
        this.paymentCycleService = paymentCycleService;
        this.eligibilityDecisionHandler = eligibilityDecisionHandler;
    }

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
        PaymentCycle currentPaymentCycle = messageContext.getCurrentPaymentCycle();
        PaymentCycle previousPaymentCycle = messageContext.getPreviousPaymentCycle();

        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateClaimantForPaymentCycle(
                claim.getClaimant(),
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle);

        paymentCycleService.updatePaymentCycle(currentPaymentCycle, decision);
        handleDecision(claim, previousPaymentCycle, currentPaymentCycle, decision, message.getCreatedTimestamp());
        if (claim.getClaimStatus() == PENDING_EXPIRY) {
            paymentCycleService.updateEndDateForClaimBecomingPendingExpiry(currentPaymentCycle);
        }
        return COMPLETED;
    }

    private void handleDecision(Claim claim,
                                PaymentCycle previousPaymentCycle,
                                PaymentCycle currentPaymentCycle,
                                EligibilityAndEntitlementDecision decision,
                                LocalDateTime messageTimestamp) {
        if (decision.getEligibilityStatus() == ELIGIBLE) {
            eligibilityDecisionHandler.handleEligibleDecision(claim, currentPaymentCycle);
        } else if (claim.getClaimStatus() == ACTIVE) {
            eligibilityDecisionHandler.handleIneligibleDecisionForActiveClaim(claim, previousPaymentCycle, currentPaymentCycle, decision);
        } else if (claimHasBeenPendingExpiryForLongerThanTheMaximumDuration(claim, messageTimestamp)) {
            eligibilityDecisionHandler.expirePendingExpiryClaim(claim);
        }
    }

    private boolean claimHasBeenPendingExpiryForLongerThanTheMaximumDuration(Claim claim, LocalDateTime messageTimestamp) {
        // !isAfter has the same effect as (isBefore || isEqual). Unfortunately isBeforeOrEqual does not exist on LocalDateTime.
        return claim.getClaimStatus() == PENDING_EXPIRY
                && !claim.getClaimStatusTimestamp().isAfter(messageTimestamp.minus(maximumPendingExpiryDuration));
    }
}
