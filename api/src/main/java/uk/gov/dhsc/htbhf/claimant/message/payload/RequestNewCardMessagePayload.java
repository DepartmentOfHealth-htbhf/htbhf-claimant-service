package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

import java.util.UUID;

@Data
@Builder
public class RequestNewCardMessagePayload implements MessagePayload {

    private UUID claimId;
    private EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision;
}
