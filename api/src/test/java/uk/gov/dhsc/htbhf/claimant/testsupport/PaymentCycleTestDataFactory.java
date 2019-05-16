package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aValidPaymentCycleVoucherEntitlement;

public class PaymentCycleTestDataFactory {

    private static final int TOTAL_VOUCHERS = 1;
    private static final int TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE = 310;


    public static PaymentCycle aValidPaymentCycle() {
        return aValidPaymentCycleBuilder().build();
    }

    public static PaymentCycle aPaymentCycleWithCycleStartDateEntitlementAndClaim(LocalDate startDate,
                                                                                  PaymentCycleVoucherEntitlement paymentCycleVoucherEntitlement,
                                                                                  Claim claim) {
        return aValidPaymentCycleBuilder()
                .cycleStartDate(startDate)
                .voucherEntitlement(paymentCycleVoucherEntitlement)
                .claim(claim)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithCycleStartDateAndClaim(LocalDate startDate, Claim claim) {
        return aValidPaymentCycleBuilder()
                .cycleStartDate(startDate)
                .claim(claim)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithPaymentAndClaim(Payment payment, Claim claim) {
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .claim(claim)
                .build();
        paymentCycle.addPayment(payment);
        return paymentCycle;
    }

    public static PaymentCycle aPaymentCycleWithClaim(Claim claim) {
        return aValidPaymentCycleBuilder().claim(claim).build();
    }

    public static PaymentCycle.PaymentCycleBuilder aValidPaymentCycleBuilder() {
        return PaymentCycle.builder()
                .claim(aValidClaim())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .voucherEntitlement(aValidPaymentCycleVoucherEntitlement())
                .totalVouchers(TOTAL_VOUCHERS)
                .totalEntitlementAmountInPence(TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE);
    }
}
