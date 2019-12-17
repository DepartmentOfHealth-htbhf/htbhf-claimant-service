package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.LetterType;

import java.util.Map;

@Value
@Builder
public class LetterMessageContext {
    private Claim claim;
    private LetterType letterType;
    private Map<String, Object> personalisation;
}
