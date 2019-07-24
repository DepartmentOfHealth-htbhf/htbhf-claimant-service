package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class EmailMessagePayload {
    private UUID claimId;
    private EmailType emailType;
    private Map<String, Object> emailPersonalisation;
}
