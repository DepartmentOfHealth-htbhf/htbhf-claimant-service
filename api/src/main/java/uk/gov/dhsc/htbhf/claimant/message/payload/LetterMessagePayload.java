package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class LetterMessagePayload implements MessagePayload {
    private UUID claimId;
    private LetterType letterType;
    private Map<String, Object> personalisation;
}
