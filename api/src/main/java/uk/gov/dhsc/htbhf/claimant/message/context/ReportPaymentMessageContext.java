package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Value
@Builder
public class ReportPaymentMessageContext {
    private Claim claim;
    private Optional<PaymentCycle> paymentCycle;
    private List<LocalDate> datesOfBirthOfChildren;
    private PaymentAction paymentAction;
    private LocalDateTime timestamp;
}
