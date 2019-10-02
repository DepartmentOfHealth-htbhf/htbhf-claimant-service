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

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlement(LocalDate startDate, List<LocalDate> childrensDob) {
        return aPaymentCycleVoucherEntitlement(startDate, childrensDob, null);
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlement(LocalDate startDate, List<LocalDate> childrensDob, LocalDate dueDate) {
        return buildPaymentCycleVoucherEntitlement(
                aVoucherEntitlement(startDate, childrensDob, dueDate),
                aVoucherEntitlement(startDate.plusWeeks(1), childrensDob, dueDate),
                aVoucherEntitlement(startDate.plusWeeks(2), childrensDob, dueDate),
                aVoucherEntitlement(startDate.plusWeeks(3), childrensDob, dueDate)
        );
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithBackdatedVouchersForYoungestChild(
            LocalDate startDate, List<LocalDate> childrensDob) {
        LocalDate birthdate = childrensDob.stream().max(LocalDate::compareTo).get();
        return PaymentCycleVoucherEntitlement.builder()
                .backdatedVouchers((int) ChronoUnit.WEEKS.between(birthdate, startDate))
                .voucherEntitlements(Arrays.asList(
                        aVoucherEntitlement(startDate, childrensDob, null),
                        aVoucherEntitlement(startDate.plusWeeks(1), childrensDob, null),
                        aVoucherEntitlement(startDate.plusWeeks(2), childrensDob, null),
                        aVoucherEntitlement(startDate.plusWeeks(3), childrensDob, null)
                ))
                .build();
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
