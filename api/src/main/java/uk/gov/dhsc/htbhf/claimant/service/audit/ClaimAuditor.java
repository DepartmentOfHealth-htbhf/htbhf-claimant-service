package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.logging.Event;
import uk.gov.dhsc.htbhf.logging.EventLogger;

import java.time.LocalDateTime;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIMANT_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_STATUS;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.ELIGIBILITY_STATUS;

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
        Map<String, Object> newClaimMetadata = Map.of(
                CLAIMANT_ID.getKey(), claim.getId(),
                CLAIM_STATUS.getKey(), claim.getClaimStatus(),
                ELIGIBILITY_STATUS.getKey(), claim.getEligibilityStatus());
        Event newClaimEvent = Event.builder()
                .eventType(ClaimEventType.NEW_CLAIM)
                .timestamp(LocalDateTime.now())
                .eventMetadata(newClaimMetadata)
                .build();
        eventLogger.logEvent(newClaimEvent);
    }
}
