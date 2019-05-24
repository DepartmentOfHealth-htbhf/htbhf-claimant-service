package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.logging.EventLogger;

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
        NewClaimEvent newClaimEvent = NewClaimEvent.builder()
                .claimId(claim.getId())
                .claimStatus(claim.getClaimStatus())
                .eligibilityStatus(claim.getEligibilityStatus())
                .build();
        eventLogger.logEvent(newClaimEvent);
    }

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

    public void auditMakePayment(UUID claimId, UUID paymentId, String paymentReference) {
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(claimId)
                .paymentId(paymentId)
                .reference(paymentReference)
                .build();
        eventLogger.logEvent(event);
    }

    public void auditNoPayment(UUID claimId) {
        NoPaymentEvent event = NoPaymentEvent.builder()
                .claimId(claimId)
                .build();
        eventLogger.logEvent(event);
    }
}
