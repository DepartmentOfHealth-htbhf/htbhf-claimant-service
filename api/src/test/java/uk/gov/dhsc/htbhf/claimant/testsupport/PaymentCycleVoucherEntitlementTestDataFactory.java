package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.*;

public class PaymentCycleVoucherEntitlementTestDataFactory {

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithVouchers() {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementWithEntitlementDate(LocalDate.now()),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(1)),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(2)),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().plusWeeks(3))
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithVouchersFromDate(LocalDate startDate) {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementWithEntitlementDate(startDate),
                aVoucherEntitlementWithEntitlementDate(startDate.plusWeeks(1)),
                aVoucherEntitlementWithEntitlementDate(startDate.plusWeeks(2)),
                aVoucherEntitlementWithEntitlementDate(startDate.plusWeeks(3))
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementMatchingChildren(LocalDate startDate, List<LocalDate> childrensDob) {
        return aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(startDate, childrensDob, null);
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementMatchingChildrenAndPregnancy(
            LocalDate startDate, List<LocalDate> childrensDob, LocalDate dueDate) {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementMatchingChildrenAndPregnancy(startDate, childrensDob, dueDate),
                aVoucherEntitlementMatchingChildrenAndPregnancy(startDate.plusWeeks(1), childrensDob, dueDate),
                aVoucherEntitlementMatchingChildrenAndPregnancy(startDate.plusWeeks(2), childrensDob, dueDate),
                aVoucherEntitlementMatchingChildrenAndPregnancy(startDate.plusWeeks(3), childrensDob, dueDate)
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(
            LocalDate startDate, List<LocalDate> childrensDob) {
        LocalDate birthdate = getDateOfBirthOfYoungestChild(childrensDob);
        return PaymentCycleVoucherEntitlement.builder()
                .backdatedVouchers((int) ChronoUnit.WEEKS.between(birthdate, startDate))
                .voucherEntitlements(Arrays.asList(
                        aVoucherEntitlementMatchingChildren(startDate, childrensDob),
                        aVoucherEntitlementMatchingChildren(startDate.plusWeeks(1), childrensDob),
                        aVoucherEntitlementMatchingChildren(startDate.plusWeeks(2), childrensDob),
                        aVoucherEntitlementMatchingChildren(startDate.plusWeeks(3), childrensDob)
                ))
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithBackdatedVouchersOnly() {
        return aPaymentCycleVoucherEntitlementWithZeroVouchers()
                .toBuilder()
                .backdatedVouchers(1)
                .build();
    }

    private static LocalDate getDateOfBirthOfYoungestChild(List<LocalDate> childrensDob) {
        return childrensDob.stream().max(LocalDate::compareTo).get();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithPregnancyVouchers() {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now()),
                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().plusWeeks(1)),
                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().plusWeeks(2)),
                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().plusWeeks(3))
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers() {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now()),
                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().plusWeeks(1)),
                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().plusWeeks(2)),
                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().plusWeeks(3))
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithZeroVouchers() {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlementWithZeroVouchers(LocalDate.now()),
                aVoucherEntitlementWithZeroVouchers(LocalDate.now().plusWeeks(1)),
                aVoucherEntitlementWithZeroVouchers(LocalDate.now().plusWeeks(2)),
                aVoucherEntitlementWithZeroVouchers(LocalDate.now().plusWeeks(3))
        );
    }

    private static PaymentCycleVoucherEntitlement buildPaymentCycleVoucherEntitlement(VoucherEntitlement... entitlements) {
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(Arrays.asList(entitlements))
                .build();
    }

}
