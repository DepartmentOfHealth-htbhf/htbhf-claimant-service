package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIMANT_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_STATUS;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.ELIGIBILITY_STATUS;

public class NewClaimEvent extends Event {

    @Builder
    public NewClaimEvent(UUID claimantId, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        super(ClaimEventType.NEW_CLAIM,
                LocalDateTime.now(),
                constructMetadata(claimantId, claimStatus, eligibilityStatus));
    }

    private static Map<String, Object> constructMetadata(UUID claimantId, ClaimStatus claimStatus, EligibilityStatus eligibilityStatus) {
        return Map.of(
                CLAIMANT_ID.getKey(), claimantId,
                CLAIM_STATUS.getKey(), claimStatus,
                ELIGIBILITY_STATUS.getKey(), eligibilityStatus
        );
    }

}
