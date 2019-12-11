package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ReportPaymentMessagePayload implements MessagePayload {
    private UUID claimId;
    private UUID paymentCycleId;
    private CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse;
    private PaymentAction paymentAction;
    private int paymentForPregnancy;
    private int paymentForChildrenUnderOne;
    private int paymentForChildrenBetweenOneAndFour;
    private int paymentForBackdatedVouchers;
    private LocalDateTime timestamp;
}
