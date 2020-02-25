package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.TextType;

import java.util.Map;

@Data
@Builder
public class TextMessageContext {
    private Claim claim;
    private TextType textType;
    private Map<String, Object> textPersonalisation;
}
