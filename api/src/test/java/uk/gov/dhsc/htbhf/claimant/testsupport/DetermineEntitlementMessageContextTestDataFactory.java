package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.context.DetermineEntitlementMessageContext;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

public class DetermineEntitlementMessageContextTestDataFactory {

    public static DetermineEntitlementMessageContext aDetermineEntitlementMessageContext(PaymentCycle currentPaymentCycle,
                                                                                         PaymentCycle previousPaymentCycle,
                                                                                         Claim claim) {
        return defaultBuilder()
                .currentPaymentCycle(currentPaymentCycle)
                .previousPaymentCycle(previousPaymentCycle)
                .claim(claim)
                .build();
    }

    private static DetermineEntitlementMessageContext.DetermineEntitlementMessageContextBuilder defaultBuilder() {
        return DetermineEntitlementMessageContext.builder()
                .claim(aValidClaim())
                .currentPaymentCycle(aValidPaymentCycle())
                .previousPaymentCycle(aValidPaymentCycle());
    }
}
