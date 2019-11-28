package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

@Value
@Builder
public class MakePaymentMessageContext {
    private Claim claim;
    private PaymentCycle paymentCycle;
    private String cardAccountId;
    private boolean paymentRestarted;
}
