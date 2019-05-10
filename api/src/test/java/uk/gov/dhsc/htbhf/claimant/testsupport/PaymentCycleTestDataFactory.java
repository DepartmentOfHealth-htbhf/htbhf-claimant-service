package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aValidPaymentCycleVoucherEntitlement;

public class PaymentCycleTestDataFactory {

    private static final int TOTAL_VOUCHERS = 1;
    private static final int TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE = 310;


    public static PaymentCycle aValidPaymentCycle() {
        return aValidPaymentCycleBuilder().build();
    }

    public static PaymentCycle aPaymentCycleWithClaim(Claim claim) {
        return aValidPaymentCycleBuilder().claim(claim).build();
    }

    private static PaymentCycle.PaymentCycleBuilder aValidPaymentCycleBuilder() {
        return PaymentCycle.builder()
                .claim(aValidClaim())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .voucherEntitlement(aValidPaymentCycleVoucherEntitlement())
                .totalVouchers(TOTAL_VOUCHERS)
                .totalEntitlementAmountInPence(TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE);
    }
}
