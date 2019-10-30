package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.util.List;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReportClaimMessageContext extends ReportEventMessageContext {

    private ClaimAction claimAction;
    // TODO DW HTBHF-2011 Rename to updatedClaimantFields in own PR.
    private List<UpdatableClaimantField> updatedClaimFields;

    @Override
    public String getEventAction() {
        return claimAction.name();
    }
}
