package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_STATUS;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.ELIGIBILITY_STATUS;

public class NewClaimEvent extends Event {

    public NewClaimEvent(Claim claim) {
        this(claim.getId(), claim.getClaimStatus(), claim.getEligibilityStatus());
    }

    @Builder
    public NewClaimEvent(UUID claimId, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        super(
                ClaimEventType.NEW_CLAIM,
                LocalDateTime.now(),
                constructMetadata(claimId, claimStatus, eligibilityStatus)
        );
    }

    private static Map<String, Object> constructMetadata(UUID claimId, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        return Map.of(
                CLAIM_ID.getKey(), claimId,
                CLAIM_STATUS.getKey(), claimStatus,
                ELIGIBILITY_STATUS.getKey(), eligibilityStatus
        );
    }

}
