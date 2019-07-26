package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchersFromDate;

public class PaymentCycleTestDataFactory {

    private static final int TOTAL_VOUCHERS = 16;
    public static final int TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE = 4960;

    public static PaymentCycle aValidPaymentCycle() {
        return aValidPaymentCycleBuilder().build();
    }

    public static PaymentCycle aPaymentCycleWithCycleEntitlementAndClaim(PaymentCycleVoucherEntitlement paymentCycleVoucherEntitlement,
                                                                         Claim claim) {
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(paymentCycleVoucherEntitlement)
                .claim(claim)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithStartDateAndClaim(LocalDate startDate,
                                                                  Claim claim) {
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchersFromDate(startDate))
                .cycleStartDate(startDate)
                .claim(claim)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithStartAndEndDate(LocalDate startDate, LocalDate endDate) {
        return aValidPaymentCycleBuilder()
                .cycleStartDate(startDate)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchersFromDate(startDate))
                .cycleEndDate(endDate)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithPregnancyVouchersOnly(LocalDate startDate, LocalDate endDate) {
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithPregnancyVouchers())
                .cycleStartDate(startDate)
                .cycleEndDate(endDate)
                .totalEntitlementAmountInPence(1240)
                .totalVouchers(4)
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

    public static PaymentCycle aPaymentCycleWithStatus(PaymentCycleStatus status) {
        return aValidPaymentCycleBuilder().paymentCycleStatus(status).build();
    }

    public static PaymentCycle.PaymentCycleBuilder aValidPaymentCycleBuilder() {
        return PaymentCycle.builder()
                .claim(aValidClaim())
                .paymentCycleStatus(NEW)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithVouchers())
                .cycleStartDate(LocalDate.now())
                .cycleEndDate(LocalDate.now().plusWeeks(4))
                .totalVouchers(TOTAL_VOUCHERS)
                .totalEntitlementAmountInPence(TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE);
    }
}
