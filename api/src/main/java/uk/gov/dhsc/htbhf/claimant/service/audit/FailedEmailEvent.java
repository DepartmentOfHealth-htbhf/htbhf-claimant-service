package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.EMAIL_TEMPLATE_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.EMAIL_TYPE;

public class FailedEmailEvent extends Event {

    @Builder
    public FailedEmailEvent(UUID claimId, String templateId, EmailType emailType) {
        super(ClaimEventType.FAILED_EMAIL, LocalDateTime.now(), constructMetaData(claimId, templateId, emailType));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, String templateId, EmailType emailType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CLAIM_ID.getKey(), claimId);
        metadata.put(EMAIL_TYPE.getKey(), emailType.name());
        metadata.put(EMAIL_TEMPLATE_ID.getKey(), templateId);
        return metadata;
    }
}
