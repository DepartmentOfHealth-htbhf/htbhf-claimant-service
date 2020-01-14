package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

@Value
@Builder
public class CompletePaymentMessageContext {
    private PaymentCycle paymentCycle;
    private Payment payment;
    private String referenceId;
}
