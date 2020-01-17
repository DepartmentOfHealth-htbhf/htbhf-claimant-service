package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;

import java.util.UUID;

@Value
@Builder
public class CompletePaymentMessagePayload implements MessagePayload {
    private UUID paymentCycleId;
    private UUID claimId;
    private PaymentResult paymentResult;
    private PaymentCalculation paymentCalculation;
    private PaymentType paymentType;
}
