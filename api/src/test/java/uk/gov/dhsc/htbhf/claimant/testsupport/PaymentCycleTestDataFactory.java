package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithBackdatedVouchersOnly;
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
        List<LocalDate> childrenDobs = nullSafeGetChildrenDob(claim);
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(paymentCycleVoucherEntitlement)
                .totalVouchers(paymentCycleVoucherEntitlement.getTotalVoucherEntitlement())
                .claim(claim)
                .childrenDob(childrenDobs)
                .expectedDeliveryDate(nullSafeGetExpectedDeliveryDate(claim))
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithStartDateAndClaim(LocalDate startDate,
                                                                  Claim claim) {
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchersFromDate(startDate);
        List<LocalDate> childrenDobs = nullSafeGetChildrenDob(claim);
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(voucherEntitlement)
                .totalVouchers(voucherEntitlement.getTotalVoucherEntitlement())
                .cycleStartDate(startDate)
                .claim(claim)
                .childrenDob(childrenDobs)
                .expectedDeliveryDate(nullSafeGetExpectedDeliveryDate(claim))
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithStartAndEndDate(LocalDate startDate, LocalDate endDate) {
        return aPaymentCycleWithStartAndEndDateAndClaim(startDate, endDate, aValidClaim());
    }

    public static PaymentCycle aPaymentCycleWithStartAndEndDateAndClaim(LocalDate startDate, LocalDate endDate, Claim claim) {
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchersFromDate(startDate);
        return aValidPaymentCycleBuilder()
                .cycleStartDate(startDate)
                .voucherEntitlement(voucherEntitlement)
                .totalVouchers(voucherEntitlement.getTotalVoucherEntitlement())
                .cycleEndDate(endDate)
                .claim(claim)
                .build();
    }

    public static PaymentCycle aPaymentCycleWithPregnancyVouchersOnly(LocalDate startDate, LocalDate endDate) {
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        List<LocalDate> childrenDobs = emptyList();
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(voucherEntitlement)
                .totalVouchers(voucherEntitlement.getTotalVoucherEntitlement())
                .cycleStartDate(startDate)
                .cycleEndDate(endDate)
                .totalEntitlementAmountInPence(voucherEntitlement.getTotalVoucherValueInPence())
                .totalVouchers(4)
                .childrenDob(childrenDobs)
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithPaymentAndClaim(Payment payment, Claim claim) {
        List<LocalDate> childrenDobs = nullSafeGetChildrenDob(claim);
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .claim(claim)
                .childrenDob(childrenDobs)
                .expectedDeliveryDate(nullSafeGetExpectedDeliveryDate(claim))
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
        paymentCycle.addPayment(payment);
        return paymentCycle;
    }

    public static PaymentCycle aPaymentCycleWithClaim(Claim claim) {
        List<LocalDate> childrenDobs = nullSafeGetChildrenDob(claim);
        return aValidPaymentCycleBuilder()
                .claim(claim)
                .childrenDob(childrenDobs)
                .expectedDeliveryDate(nullSafeGetExpectedDeliveryDate(claim))
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithStatus(PaymentCycleStatus status) {
        return aValidPaymentCycleBuilder().paymentCycleStatus(status).build();
    }

    public static PaymentCycle aPaymentCycleWithChildrenDobs(List<LocalDate> childrenDobs) {
        return aValidPaymentCycleBuilder()
                .childrenDob(childrenDobs)
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithClaimAndChildrenDobs(Claim claim, List<LocalDate> childrenDobs) {
        return aValidPaymentCycleBuilder()
                .claim(claim)
                .childrenDob(childrenDobs)
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs))
                .build();
    }

    public static PaymentCycle aPaymentCycleWithBackdatedVouchersOnly() {
        return aValidPaymentCycleBuilder()
                .voucherEntitlement(aPaymentCycleVoucherEntitlementWithBackdatedVouchersOnly())
                .build();
    }

    public static PaymentCycle.PaymentCycleBuilder aValidPaymentCycleBuilder() {
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        Claim claim = aValidClaim();
        List<LocalDate> childrenDobs = List.of(
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusYears(3).minusMonths(6));
        return PaymentCycle.builder()
                .claim(claim)
                .paymentCycleStatus(NEW)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .voucherEntitlement(voucherEntitlement)
                .totalVouchers(voucherEntitlement.getTotalVoucherEntitlement())
                .cycleStartDate(LocalDate.now())
                //Next cycle starts 4 weeks after the current one so last day of current cycle is one day less
                .cycleEndDate(LocalDate.now().plusDays(27))
                .totalVouchers(TOTAL_VOUCHERS)
                .childrenDob(childrenDobs)
                .totalEntitlementAmountInPence(TOTAL_ENTITLEMENT_AMOUNT_IN_PENCE)
                .expectedDeliveryDate(claim.getClaimant().getExpectedDeliveryDate())
                .identityAndEligibilityResponse(
                        CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(childrenDobs));
    }

    private static List<LocalDate> nullSafeGetChildrenDob(Claim claim) {
        return (claim == null) ? emptyList() : claim.getClaimant().getChildrenDob();
    }

    private static LocalDate nullSafeGetExpectedDeliveryDate(Claim claim) {
        return (claim == null) ? null : claim.getClaimant().getExpectedDeliveryDate();
    }
}
