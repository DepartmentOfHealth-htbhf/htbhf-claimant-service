package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReportClaimMessageContext extends ReportEventMessageContext {

    private ClaimAction claimAction;

    @Override
    public String getEventAction() {
        return claimAction.name();
    }
}
