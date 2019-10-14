package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ReportClaimMessageContext {

    private Claim claim;
    private List<LocalDate> datesOfBirthOfChildren;
    private ClaimAction claimAction;
    private LocalDateTime timestamp;
}
