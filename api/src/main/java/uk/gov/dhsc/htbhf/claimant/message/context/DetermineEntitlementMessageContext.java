package uk.gov.dhsc.htbhf.claimant.message.context;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

@Data
@Builder
public class DetermineEntitlementMessageContext {

    private PaymentCycle currentPaymentCycle;
    private PaymentCycle previousPaymentCycle;
    private Claim claim;

}
