package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CARD_ACCOUNT_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;

public class NewCardEvent extends Event {

    @Builder
    public NewCardEvent(UUID claimId, String cardAccountId) {
        super(ClaimEventType.NEW_CARD, LocalDateTime.now(), constructMetaData(claimId, cardAccountId));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, String cardAccountId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CLAIM_ID.getKey(), claimId);
        metadata.put(CARD_ACCOUNT_ID.getKey(), cardAccountId);
        return metadata;
    }
}
