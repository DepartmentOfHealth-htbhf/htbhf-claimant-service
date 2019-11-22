package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;

@Value
@Builder
public class CompleteNewCardMessageContext {

    private Claim claim;
    private String cardAccountId;
    private EligibilityAndEntitlementDecision eligibilityAndEntitlementDecision;
}
