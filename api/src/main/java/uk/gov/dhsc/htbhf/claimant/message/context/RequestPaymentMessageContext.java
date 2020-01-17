package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;

@Value
@Builder
public class RequestPaymentMessageContext {
    private Claim claim;
    private PaymentCycle paymentCycle;
    private PaymentType paymentType;
}
