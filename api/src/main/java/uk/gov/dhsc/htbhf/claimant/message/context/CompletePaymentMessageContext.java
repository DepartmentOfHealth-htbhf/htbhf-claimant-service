package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;

@Value
@Builder
public class CompletePaymentMessageContext {
    private PaymentCycle paymentCycle;
    private Claim claim;
    private PaymentResult paymentResult;
    private PaymentCalculation paymentCalculation;
    private PaymentType paymentType;
}
