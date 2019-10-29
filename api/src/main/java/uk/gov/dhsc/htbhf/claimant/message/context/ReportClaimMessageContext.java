package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReportClaimMessageContext extends ReportEventMessageContext {

    private ClaimAction claimAction;
    private List<UpdatableClaimantField> updatedClaimFields;

    @Override
    public String getEventAction() {
        return claimAction.name();
    }
}
