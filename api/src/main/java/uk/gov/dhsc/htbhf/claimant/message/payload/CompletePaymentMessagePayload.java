package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

@Value
@Builder
public class CompletePaymentMessagePayload implements MessagePayload {
    private PaymentCycle paymentCycle;
    private Payment payment;
    private String paymentReferenceId;
}
