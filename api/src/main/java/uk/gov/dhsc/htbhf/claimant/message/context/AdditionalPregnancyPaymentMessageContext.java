package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.util.Optional;

@Data
@Builder
public class AdditionalPregnancyPaymentMessageContext implements MessagePayload {
    private Claim claim;
    private Optional<PaymentCycle> paymentCycle;
}
