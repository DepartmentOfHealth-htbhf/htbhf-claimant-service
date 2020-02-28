package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class TextMessagePayload implements MessagePayload {
    private UUID claimId;
    private TextType textType;
    private Map<String, Object> textPersonalisation;
}
