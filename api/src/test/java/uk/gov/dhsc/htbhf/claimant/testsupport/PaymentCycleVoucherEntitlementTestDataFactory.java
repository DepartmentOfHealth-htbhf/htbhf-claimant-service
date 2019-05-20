package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aVoucherEntitlementWithZeroVouchers;

public class PaymentCycleVoucherEntitlementTestDataFactory {

    public static PaymentCycleVoucherEntitlement aPaymentCycleVoucherEntitlementWithVouchers() {
        return buildDefaultPaymentCycleVoucherEntitlement()
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
