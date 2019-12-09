package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportPaymentMessagePayload implements MessagePayload {
    private UUID claimId;
    private UUID paymentCycleId;
    private PaymentAction paymentAction;
    private int paymentForPregnancy;
    private int paymentForChildrenUnderOne;
    private int paymentForChildrenBetweenOneAndFour;
    private int paymentForBackdatedVouchers;
    private LocalDateTime timestamp;
}
