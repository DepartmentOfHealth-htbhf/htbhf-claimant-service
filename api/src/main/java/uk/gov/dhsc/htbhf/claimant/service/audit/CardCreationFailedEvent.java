package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.CARD_CREATION_FAILED;

public class CardCreationFailedEvent extends Event {

    @Builder
    public CardCreationFailedEvent(UUID claimId) {
        super(CARD_CREATION_FAILED, LocalDateTime.now(), constructMetaData(claimId));
    }

    private static Map<String, Object> constructMetaData(UUID claimId) {
        return Map.of(CLAIM_ID.getKey(), claimId);
    }
}
