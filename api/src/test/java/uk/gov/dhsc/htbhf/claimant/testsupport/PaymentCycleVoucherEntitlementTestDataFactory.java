package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithEntitlementDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithNoPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithPregnancyVoucherOnlyForDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithZeroVouchers;

public class PaymentCycleVoucherEntitlementTestDataFactory {

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithPregnancyVouchers() {
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(
                        List.of(
                                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().minusWeeks(1)),
                                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().minusWeeks(2)),
                                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().minusWeeks(3)),
                                aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate.now().minusWeeks(4))
                        )
                )
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .voucherEntitlements(
                        List.of(
                                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().minusWeeks(1)),
                                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().minusWeeks(2)),
                                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().minusWeeks(3)),
                                aVoucherEntitlementWithNoPregnancyVouchers(LocalDate.now().minusWeeks(4))
                        )
                )
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithZeroVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .voucherEntitlements(
                        List.of(
                                aVoucherEntitlementWithZeroVouchers(LocalDate.now().minusWeeks(1)),
                                aVoucherEntitlementWithZeroVouchers(LocalDate.now().minusWeeks(2)),
                                aVoucherEntitlementWithZeroVouchers(LocalDate.now().minusWeeks(3)),
                                aVoucherEntitlementWithZeroVouchers(LocalDate.now().minusWeeks(4))
                        )
                )
                .build();
    }

    private static PaymentCycleVoucherEntitlement.PaymentCycleVoucherEntitlementBuilder buildDefaultPaymentCycleVoucherEntitlement() {
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(
                        List.of(
                                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(1)),
                                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(2)),
                                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(3)),
                                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(4))
                        )
                );
    }
}
