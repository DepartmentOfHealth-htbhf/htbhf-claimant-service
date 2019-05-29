package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.*;

public class PaymentCycleVoucherEntitlementTestDataFactory {

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithPregnancyVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .voucherEntitlements(singletonList(
                        aVoucherEntitlementWithPregnancyVouchers(4)
                ))
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .voucherEntitlements(singletonList(
                        aVoucherEntitlementWithPregnancyVouchers(0)
                ))
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers() {
        List<VoucherEntitlement> entitlements = List.of(
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(4)),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(2)),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(1)),
                aVoucherEntitlementWithEntitlementDate(LocalDate.now().minusWeeks(3))
        );
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(entitlements)
                .build();
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithZeroVouchers() {
        return aPaymentCycleVoucherEntitlementWithEntitlement(aVoucherEntitlementWithZeroVouchers());
    }

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithEntitlement(VoucherEntitlement voucherEntitlement) {
        return buildDefaultPaymentCycleVoucherEntitlement()
                .voucherEntitlements(singletonList(voucherEntitlement))
                .build();
    }

    private static PaymentCycleVoucherEntitlement.PaymentCycleVoucherEntitlementBuilder buildDefaultPaymentCycleVoucherEntitlement() {
        return PaymentCycleVoucherEntitlement.builder()
                .voucherEntitlements(singletonList(aValidVoucherEntitlement()));
    }
}
