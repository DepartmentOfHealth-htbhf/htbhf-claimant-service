package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

import java.time.LocalDate;
import java.util.List;

public class MessagePayloadFactory {

    public static NewCardRequestMessagePayload buildNewCardMessagePayload(Claim claim,
                                                                          PaymentCycleVoucherEntitlement voucherEntitlement,
                                                                          List<LocalDate> datesOfBirthOfChildren) {
        return NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .voucherEntitlement(voucherEntitlement)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .build();
    }

    public static MakePaymentMessagePayload buildMakePaymentMessagePayload(PaymentCycle paymentCycle) {
        return MakePaymentMessagePayload.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentCycleId(paymentCycle.getId())
                .cardAccountId(paymentCycle.getClaim().getCardAccountId())
                .build();
    }
}
