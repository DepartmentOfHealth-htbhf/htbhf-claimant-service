package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.communications.DetermineEntitlementNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.*;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.ClaimMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.EligibilityAndEntitlementService;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDate;
import java.util.List;
import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.DETERMINE_ENTITLEMENT;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_EXPIRED;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

@Slf4j
@Component
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class DetermineEntitlementMessageProcessor implements MessageTypeProcessor {

    private EligibilityAndEntitlementService eligibilityAndEntitlementService;

    private MessageContextLoader messageContextLoader;

    private PaymentCycleService paymentCycleService;

    private ClaimRepository claimRepository;

    private MessageQueueClient messageQueueClient;

    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;

    private PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    private EventAuditor eventAuditor;

    private ClaimMessageSender claimMessageSender;

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

        EligibilityAndEntitlementDecision decision = eligibilityAndEntitlementService.evaluateExistingClaimant(
                claim.getClaimant(),
                currentPaymentCycle.getCycleStartDate(),
                previousPaymentCycle);

        paymentCycleService.updatePaymentCycle(currentPaymentCycle, decision);
        handleDecision(claim, previousPaymentCycle, currentPaymentCycle, decision);
        return COMPLETED;
    }

    private void handleDecision(Claim claim, PaymentCycle previousPaymentCycle, PaymentCycle currentPaymentCycle, EligibilityAndEntitlementDecision decision) {
        if (decision.getEligibilityStatus() == ELIGIBLE) {
            createMakePaymentMessage(currentPaymentCycle);
        } else if (claim.getClaimStatus() == ACTIVE) {
            if (shouldExpireClaim(decision, previousPaymentCycle, currentPaymentCycle)) {
                expireActiveClaim(claim, decision.getDateOfBirthOfChildren());
            } else if (decision.getQualifyingBenefitEligibilityStatus().isNotEligible()) {
                handleLossOfQualifyingBenefitStatus(claim, decision.getDateOfBirthOfChildren());
            } else {
                handleNoLongerEligibleForSchemeAsNoChildrenAndNotPregnant(claim, decision.getDateOfBirthOfChildren());
            }

            setCardStatusToPendingCancellation(claim);
        }
        //TODO HTBHF-1296: If not ACTIVE, PENDING_EXPIRY will be moved to EXPIRED after 16 weeks.
    }

    private boolean shouldExpireClaim(EligibilityAndEntitlementDecision decision, PaymentCycle previousPaymentCycle, PaymentCycle currentPaymentCycle) {
        if (decision.childrenPresent() || claimantIsPregnantInCycle(currentPaymentCycle)) {
            return false;
        }
        if (childrenExistedInPreviousCycleAndNowOver4(previousPaymentCycle, currentPaymentCycle)) {
            return true;
        }
        return claimantIsPregnantInCycle(previousPaymentCycle);
    }

    private boolean childrenExistedInPreviousCycleAndNowOver4(PaymentCycle previousPaymentCycle, PaymentCycle currentPaymentCycle) {
        return childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(previousPaymentCycle)
                && !childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(previousPaymentCycle.getChildrenDob(), currentPaymentCycle.getCycleStartDate());
    }

    //Use the PregnancyEntitlementCalculator to check that the claimant is either not pregnant or their pregnancy date is
    //considered too far in the past.
    private boolean claimantIsPregnantInCycle(PaymentCycle paymentCycle) {
        return pregnancyEntitlementCalculator.isEntitledToVoucher(
                paymentCycle.getExpectedDeliveryDate(),
                paymentCycle.getCycleStartDate());
    }

    private void createMakePaymentMessage(PaymentCycle paymentCycle) {
        MessagePayload messagePayload = MessagePayloadFactory.buildMakePaymentMessagePayload(paymentCycle);
        messageQueueClient.sendMessage(messagePayload, MessageType.MAKE_PAYMENT);
    }

    private void handleLossOfQualifyingBenefitStatus(Claim claim, List<LocalDate> dateOfBirthOfChildren) {
        updateClaimStatus(claim, ClaimStatus.PENDING_EXPIRY);
        determineEntitlementNotificationHandler.sendClaimNoLongerEligibleEmail(claim);
        claimMessageSender.sendReportClaimMessage(claim, dateOfBirthOfChildren, UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY);
    }

    private void handleNoLongerEligibleForSchemeAsNoChildrenAndNotPregnant(Claim claim, List<LocalDate> dateOfBirthOfChildren) {
        expireActiveClaim(claim, dateOfBirthOfChildren);
        determineEntitlementNotificationHandler.sendNoChildrenOnFeedClaimNoLongerEligibleEmail(claim);
    }

    private void expireActiveClaim(Claim claim, List<LocalDate> dateOfBirthOfChildren) {
        updateClaimStatus(claim, ClaimStatus.EXPIRED);
        eventAuditor.auditExpiredClaim(claim);
        claimMessageSender.sendReportClaimMessage(claim, dateOfBirthOfChildren, UPDATED_FROM_ACTIVE_TO_EXPIRED);
    }

    private void setCardStatusToPendingCancellation(Claim claim) {
        claim.updateCardStatus(PENDING_CANCELLATION);
        claimRepository.save(claim);
    }

    private void updateClaimStatus(Claim claim, ClaimStatus claimStatus) {
        claim.updateClaimStatus(claimStatus);
        claimRepository.save(claim);
    }

}
