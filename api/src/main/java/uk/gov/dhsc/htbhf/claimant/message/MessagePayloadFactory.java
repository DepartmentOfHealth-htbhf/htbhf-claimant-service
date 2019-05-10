package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

public class MessagePayloadFactory {

    public static NewCardRequestMessagePayload buildNewCardMessagePayload(Claim claim, PaymentCycleVoucherEntitlement voucherEntitlement) {
        return NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .voucherEntitlement(voucherEntitlement)
                .build();
    }
}
