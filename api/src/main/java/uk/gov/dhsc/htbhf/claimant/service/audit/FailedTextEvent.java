package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextType;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.TEMPLATE_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.TEXT_TYPE;

public class FailedTextEvent extends Event {

    @Builder
    public FailedTextEvent(UUID claimId, String templateId, TextType textType) {
        super(ClaimEventType.FAILED_TEXT, LocalDateTime.now(), constructMetaData(claimId, templateId, textType));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, String templateId, TextType textType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CLAIM_ID.getKey(), claimId);
        metadata.put(TEXT_TYPE.getKey(), textType.name());
        metadata.put(TEMPLATE_ID.getKey(), templateId);
        return metadata;
    }
}
