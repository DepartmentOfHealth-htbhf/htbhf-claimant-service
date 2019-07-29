package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.util.Map;

@Data
@Builder
public class EmailMessageContext {
    private Claim claim;
    private String templateId;
    private EmailType emailType;
    private Map<String, Object> emailPersonalisation;
}
