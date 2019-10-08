package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

@Value
@Builder
public class ReportClaimMessageContext {

    private Claim claim;
}
