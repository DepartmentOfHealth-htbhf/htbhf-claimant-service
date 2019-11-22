package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javers.common.collections.Lists;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.logging.EventLogger;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.List;
import java.util.UUID;

/**
 * Component responsible for auditing events around a Claim.
 */
@Component
@AllArgsConstructor
@Slf4j
public class EventAuditor {

    private final EventLogger eventLogger;

    /**
     * Audit a new claim event.
     *
     * @param claim The claim to audit
     */
    public void auditNewClaim(Claim claim) {
        if (claim == null) {
            log.warn("Unable to audit null Claim");
            return;
        }
        NewClaimEvent newClaimEvent = new NewClaimEvent(claim);
        eventLogger.logEvent(newClaimEvent);
    }

    /**
     * Audit that a claim has been updated.
     *
     * @param claim         the claim that has been updated
     * @param updatedFields the fields on the claim that have been updated
     */
    public void auditUpdatedClaim(Claim claim, List<UpdatableClaimantField> updatedFields) {
        List<String> updatedFieldsAsStrings = Lists.transform(updatedFields, UpdatableClaimantField::getFieldName);
        UpdatedClaimEvent updatedClaimEvent = UpdatedClaimEvent.builder()
                .claimId(claim.getId())
                .updatedFields(updatedFieldsAsStrings)
                .build();
        eventLogger.logEvent(updatedClaimEvent);
    }

    /**
     * Audit a new card event given a card response.
     *
     * @param claimId      The claim id which must not be null
     * @param cardAccountId The card account id which must not be null
     */
    public void auditNewCard(UUID claimId, String cardAccountId) {
        if (claimId == null || cardAccountId == null) {
            log.warn("Unable to audit new card event with claimId: {} and cardAccountId: {}. Both fields must be provided", claimId, cardAccountId);
            return;
        }
        NewCardEvent newCardEvent = NewCardEvent.builder()
                .claimId(claimId)
                .cardAccountId(cardAccountId)
                .build();
        eventLogger.logEvent(newCardEvent);
    }

    public void auditMakePayment(PaymentCycle paymentCycle, Payment payment, DepositFundsResponse depositFundsResponse) {
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentAmountInPence(payment.getPaymentAmountInPence())
                .paymentId(payment.getId())
                .reference(depositFundsResponse.getReferenceId())
                .build();
        eventLogger.logEvent(event);
    }

    public void auditBalanceTooHighForPayment(PaymentCycle paymentCycle) {
        BalanceTooHighForPaymentEvent event = BalanceTooHighForPaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .balanceOnCard(paymentCycle.getCardBalanceInPence())
                .build();
        eventLogger.logEvent(event);
    }

    /**
     * Audit a failure event.
     *
     * @param failureEvent The event that has failed
     */
    public void auditFailedEvent(FailureEvent failureEvent) {
        eventLogger.logEvent(failureEvent);
    }

    /**
     * Audits when a claim has been expired.
     *
     * @param claim The claim that has been expired
     */
    public void auditExpiredClaim(Claim claim) {
        if (claim == null) {
            log.warn("Unable to audit expired claim event, claim is null");
            return;
        }
        ExpiredClaimEvent expiredClaimEvent = ExpiredClaimEvent.builder().claimId(claim.getId()).build();
        eventLogger.logEvent(expiredClaimEvent);
    }
}
