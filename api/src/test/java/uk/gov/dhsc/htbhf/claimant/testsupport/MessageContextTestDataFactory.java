package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.NewCardMessageContext;

import java.util.Optional;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

public class MessageContextTestDataFactory {

    public static DetermineEntitlementMessageContext aDetermineEntitlementMessageContext(PaymentCycle currentPaymentCycle,
                                                                                         PaymentCycle previousPaymentCycle,
                                                                                         Claim claim) {
        return DetermineEntitlementMessageContext.builder()
                .currentPaymentCycle(currentPaymentCycle)
                .previousPaymentCycle(previousPaymentCycle)
                .claim(claim)
                .build();
    }

    public static MakePaymentMessageContext aValidMakePaymentMessageContext(PaymentCycle paymentCycle, Claim claim) {
        return MakePaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .cardAccountId(claim.getCardAccountId())
                .build();
    }


    public static NewCardMessageContext aValidNewCardMessageContext() {
        return NewCardMessageContext.builder()
                .claim(aValidClaim())
                .paymentCycleVoucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .build();
    }

    public static AdditionalPregnancyPaymentMessageContext aValidAdditionalPregnancyPaymentMessageContext(Claim claim, Optional<PaymentCycle> paymentCycle) {
        return AdditionalPregnancyPaymentMessageContext.builder()
                .claim(claim)
                .paymentCycle(paymentCycle)
                .build();
    }
}
