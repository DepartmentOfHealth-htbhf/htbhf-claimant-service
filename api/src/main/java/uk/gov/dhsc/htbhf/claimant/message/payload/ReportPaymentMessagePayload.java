package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
public class ReportPaymentMessagePayload implements MessagePayload {
    private UUID claimId;
    private Optional<UUID> paymentCycleId;
    private List<LocalDate> datesOfBirthOfChildren;
    private PaymentAction paymentAction;
    private LocalDateTime timestamp;
}
