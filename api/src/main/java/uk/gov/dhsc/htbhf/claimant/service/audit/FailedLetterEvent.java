package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.LETTER_TYPE;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.TEMPLATE_ID;

public class FailedLetterEvent extends Event {

    @Builder
    public FailedLetterEvent(UUID claimId, String templateId, LetterType letterType) {
        super(ClaimEventType.FAILED_LETTER, LocalDateTime.now(), constructMetaData(claimId, templateId, letterType));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, String templateId, LetterType letterType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CLAIM_ID.getKey(), claimId);
        metadata.put(LETTER_TYPE.getKey(), letterType.name());
        metadata.put(TEMPLATE_ID.getKey(), templateId);
        return metadata;
    }
}
