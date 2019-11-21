package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

@Data
@Builder
public class RequestNewCardMessageContext {

    private Claim claim;
    private EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision;
}
