package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;

public class ExpiredClaimEvent extends Event {

    @Builder
    public ExpiredClaimEvent(UUID claimId) {
        super(ClaimEventType.EXPIRED_CLAIM, LocalDateTime.now(), constructMetaData(claimId));
    }

    private static Map<String, Object> constructMetaData(UUID claimId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CLAIM_ID.getKey(), claimId);
        return metadata;
    }
}
