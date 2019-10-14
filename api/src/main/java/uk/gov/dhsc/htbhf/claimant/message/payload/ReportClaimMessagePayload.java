package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReportClaimMessagePayload implements MessagePayload {
    private UUID claimId;
    private List<LocalDate> datesOfBirthOfChildren;
    private ClaimAction claimAction;
    private LocalDateTime timestamp;
}
