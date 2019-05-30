package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.UPDATED_FIELDS;

public class UpdatedClaimEvent extends Event {

    @Builder
    public UpdatedClaimEvent(UUID claimId, List<String> updatedFields) {
        super(
                ClaimEventType.UPDATED_CLAIM,
                LocalDateTime.now(),
                constructMetadata(claimId, updatedFields)
        );
    }

    private static Map<String, Object> constructMetadata(UUID claimId, List<String> updatedFields) {
        return Map.of(
                CLAIM_ID.getKey(), claimId,
                UPDATED_FIELDS.getKey(), updatedFields
        );
    }

}
