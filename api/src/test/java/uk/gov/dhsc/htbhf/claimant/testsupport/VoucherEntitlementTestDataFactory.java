package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_PREGNANCY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

public class VoucherEntitlementTestDataFactory {

    public static VoucherEntitlement aValidVoucherEntitlement() {
        return aVoucherEntitlementBuilder()
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithZeroVouchers() {
        return VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(0)
                .vouchersForChildrenBetweenOneAndFour(0)
                .vouchersForPregnancy(0)
                .entitlementDate(LocalDate.now())
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithEntitlementDate(LocalDate entitlementDate) {
        return aVoucherEntitlementBuilder()
                .entitlementDate(entitlementDate)
                .build();
    }

    private static VoucherEntitlement.VoucherEntitlementBuilder aVoucherEntitlementBuilder() {
        return VoucherEntitlement.builder()
                .voucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(VOUCHERS_FOR_CHILDREN_UNDER_ONE)
                .vouchersForChildrenBetweenOneAndFour(VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR)
                .vouchersForPregnancy(VOUCHERS_FOR_PREGNANCY)
                .entitlementDate(LocalDate.now());
    }

}
