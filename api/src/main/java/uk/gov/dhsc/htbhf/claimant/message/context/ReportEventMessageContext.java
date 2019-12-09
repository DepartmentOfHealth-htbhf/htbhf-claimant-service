package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
public abstract class ReportEventMessageContext {

    private Claim claim;
    private LocalDateTime timestamp;

    public abstract String getEventAction();
}
