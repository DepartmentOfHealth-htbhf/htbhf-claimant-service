package uk.gov.dhsc.htbhf.claimant.service.audit;

import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.*;

public class ClaimExpiredEvent extends Event {

    public ClaimExpiredEvent(UUID claimId) {
        super(ClaimEventType.CLAIM_EXPIRED, LocalDateTime.now(), Map.of(CLAIM_ID.getKey(), claimId));
    }
}
