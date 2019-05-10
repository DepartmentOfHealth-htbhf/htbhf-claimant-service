package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import java.util.UUID;

@Data
@Builder
public class NewCardRequestMessagePayload implements MessagePayload {

    private UUID claimId;
    private PaymentCycleVoucherEntitlement voucherEntitlement;
}
