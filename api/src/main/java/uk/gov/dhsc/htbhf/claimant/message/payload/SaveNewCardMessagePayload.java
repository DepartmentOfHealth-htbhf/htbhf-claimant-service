package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import java.util.UUID;

@Value
@Builder
public class SaveNewCardMessagePayload implements MessagePayload {

    private UUID claimId;
    private EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision;
    private String cardAccountId;
}
