package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
public abstract class ReportEventMessageContext {

    private Claim claim;
    private CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse;
    private LocalDateTime timestamp;

    public abstract String getEventAction();
}
