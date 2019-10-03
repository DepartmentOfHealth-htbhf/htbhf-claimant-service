package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
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
     * @param claim the claim that has been updated
     * @param updatedFields the fields on the claim that have been updated
     */
    public void auditUpdatedClaim(Claim claim, List<String> updatedFields) {
        UpdatedClaimEvent updatedClaimEvent = UpdatedClaimEvent.builder()
                .claimId(claim.getId())
                .updatedFields(updatedFields)
                .build();
        eventLogger.logEvent(updatedClaimEvent);
    }

    /**
     * Audit a new card event given a card response.
     *
     * @param claimId      The claim id
     * @param cardResponse The card response which must not be null
     */
    public void auditNewCard(UUID claimId, CardResponse cardResponse) {
        if (claimId == null || cardResponse == null) {
            log.warn("Unable to audit new card event with claimId: {} and cardResponse: {}. Both fields must not be null", claimId, cardResponse);
            return;
        }
        NewCardEvent newCardEvent = NewCardEvent.builder()
                .claimId(claimId)
                .cardAccountId(cardResponse.getCardAccountId())
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

    public void auditClaimExpired(Claim claim) {
        ClaimExpiredEvent event = new ClaimExpiredEvent(claim.getId());
        eventLogger.logEvent(event);
    }
}
