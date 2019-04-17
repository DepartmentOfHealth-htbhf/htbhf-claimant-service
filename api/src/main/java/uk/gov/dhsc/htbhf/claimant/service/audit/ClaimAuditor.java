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
public class ClaimAuditor {

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
        NewCardEvent newCardEvent = NewCardEvent.builder()
                .claimId(claimId)
                .cardAccountId(cardResponse.getCardAccountId())
                .build();
        eventLogger.logEvent(newCardEvent);
    }
}
