package uk.gov.dhsc.htbhf.claimant.eligibility;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.DetermineEntitlementNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessagePayloadFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED;

@Component
@AllArgsConstructor
public class EligibilityDecisionHandler {

    private ClaimRepository claimRepository;
    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;
    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    private EventAuditor eventAuditor;
    private ClaimMessageSender claimMessageSender;
    private final MessageQueueClient messageQueueClient;

    /**
     * Handles the processing of eligible {@link EligibilityAndEntitlementDecision}
     * This includes updating claim/card statuses and sending notifications if necessary.
     *
     * @param claim               the claim the decision relates to
     * @param currentPaymentCycle the claim's current payment cycle
     */
    public void handleEligibleDecision(Claim claim, PaymentCycle currentPaymentCycle) {
        if (claim.getClaimStatus() == ACTIVE) {
            createMakeRegularPaymentMessage(currentPaymentCycle);
        } else if (claim.getClaimStatus() == PENDING_EXPIRY) {
            updateClaimAndCardStatus(claim, ACTIVE, CardStatus.ACTIVE);
            createMakeRestartedPaymentMessage(currentPaymentCycle);
        }

        if (pregnancyEntitlementCalculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(currentPaymentCycle)) {
            // Email is worded such that we don't need to check if a new child from pregnancy has already appeared.
            claimMessageSender.sendReportABirthEmailMessage(claim);
        }
    }

    /**
     * Handles the processing of ineligible {@link EligibilityAndEntitlementDecision} for active claims.
     * This includes updating claim/card statuses and sending notifications if necessary.
     *
     * @param claim                the claim the decision relates to
     * @param previousPaymentCycle the claim's previous payment cycle
     * @param currentPaymentCycle  the claim's current payment cycle
     * @param decision             the ineligible entitlement decision
     */
    public void handleIneligibleDecisionForActiveClaim(Claim claim,
                                                       PaymentCycle previousPaymentCycle,
                                                       PaymentCycle currentPaymentCycle,
                                                       EligibilityAndEntitlementDecision decision) {

        if (shouldExpireActiveClaim(decision, previousPaymentCycle, currentPaymentCycle)) {
            expireClaim(claim, UPDATED_FROM_ACTIVE_TO_EXPIRED);
        } else if (decision.getIdentityAndEligibilityResponse().isNotEligible()) {
            handleLossOfQualifyingBenefitStatus(claim);
        } else {
            expireClaim(claim, UPDATED_FROM_ACTIVE_TO_EXPIRED);
            determineEntitlementNotificationHandler.sendNoChildrenOnFeedClaimNoLongerEligibleEmail(claim);
        }
    }

    /**
     * Expires a claim which has a status of pending expiry.
     *
     * @param claim                  the claim to expire
     * @param datesOfBirthOfChildren the dates of birth of the claimant's children
     */
    public void expirePendingExpiryClaim(Claim claim, List<LocalDate> datesOfBirthOfChildren) {
        expireClaim(claim, UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED);
    }

    private boolean shouldExpireActiveClaim(EligibilityAndEntitlementDecision decision, PaymentCycle previousPaymentCycle, PaymentCycle currentPaymentCycle) {
        if (decision.childrenPresent() || pregnancyEntitlementCalculator.claimantIsPregnantInCycle(currentPaymentCycle)) {
            return false;
        }
        if (childrenExistedInPreviousCycleAndNowOver4(previousPaymentCycle, currentPaymentCycle)) {
            return true;
        }
        return pregnancyEntitlementCalculator.claimantIsPregnantInCycle(previousPaymentCycle);
    }

    private boolean childrenExistedInPreviousCycleAndNowOver4(PaymentCycle previousPaymentCycle, PaymentCycle currentPaymentCycle) {
        return childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle)
                && !childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(previousPaymentCycle.getChildrenDob(), currentPaymentCycle.getCycleStartDate());
    }

    private void handleLossOfQualifyingBenefitStatus(Claim claim) {
        updateClaimAndCardStatus(claim, PENDING_EXPIRY, PENDING_CANCELLATION);
        determineEntitlementNotificationHandler.sendClaimNoLongerEligibleEmail(claim);
        claimMessageSender.sendReportClaimMessage(claim, UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY);
    }

    private void expireClaim(Claim claim, ClaimAction claimAction) {
        updateClaimAndCardStatus(claim, ClaimStatus.EXPIRED, PENDING_CANCELLATION);
        eventAuditor.auditExpiredClaim(claim);
        claimMessageSender.sendReportClaimMessage(claim, claimAction);
    }


    private void updateClaimAndCardStatus(Claim claim, ClaimStatus claimStatus, CardStatus cardStatus) {
        claim.updateClaimStatus(claimStatus);
        claim.updateCardStatus(cardStatus);
        claimRepository.save(claim);
    }

    private void createMakeRegularPaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    private void createMakeRestartedPaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayloadForRestartedPayment(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }
}
