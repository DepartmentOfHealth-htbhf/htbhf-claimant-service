package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHERS_FOR_PREGNANCY;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

public class VoucherEntitlementTestDataFactory {

    public static VoucherEntitlement aValidVoucherEntitlement() {
        return aVoucherEntitlementBuilder()
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithZeroVouchers(LocalDate entitlementDate) {
        return VoucherEntitlement.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(0)
                .vouchersForChildrenBetweenOneAndFour(0)
                .vouchersForPregnancy(0)
                .entitlementDate(entitlementDate)
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithVouchersForUnderOne(LocalDate entitlementDate) {
        return VoucherEntitlement.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(2)
                .vouchersForChildrenBetweenOneAndFour(0)
                .vouchersForPregnancy(1)
                .entitlementDate(entitlementDate)
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithPregnancyVoucherOnlyForDate(LocalDate entitlementDate) {
        return VoucherEntitlement.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(0)
                .vouchersForChildrenBetweenOneAndFour(0)
                .vouchersForPregnancy(1)
                .entitlementDate(entitlementDate)
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithEntitlementDate(LocalDate entitlementDate) {
        return aVoucherEntitlementBuilder()
                .entitlementDate(entitlementDate)
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlement(LocalDate entitlementDate, List<LocalDate> childrensDob, LocalDate dueDate) {
        int pregnancy = dueDate == null || dueDate.plusWeeks(12).isBefore(entitlementDate) ? 0 : 1;
        int childrenUnderOne = (int) childrensDob.stream().filter(d -> d.plusYears(1).isAfter(entitlementDate)).count();
        int childrenUnderFour = (int) childrensDob.stream().filter(d -> d.plusYears(4).isAfter(entitlementDate)).count();
        return VoucherEntitlement.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(childrenUnderOne * 2)
                .vouchersForChildrenBetweenOneAndFour(childrenUnderFour - childrenUnderOne)
                .vouchersForPregnancy(pregnancy)
                .entitlementDate(entitlementDate)
                .build();
    }

    public static VoucherEntitlement aVoucherEntitlementWithNoPregnancyVouchers(LocalDate entitlementDate) {
        return aVoucherEntitlementBuilder()
                .vouchersForPregnancy(0)
                .entitlementDate(entitlementDate)
                .build();
    }

    private static VoucherEntitlement.VoucherEntitlementBuilder aVoucherEntitlementBuilder() {
        return VoucherEntitlement.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(VOUCHERS_FOR_CHILDREN_UNDER_ONE)
                .vouchersForChildrenBetweenOneAndFour(VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR)
                .vouchersForPregnancy(VOUCHERS_FOR_PREGNANCY)
                .entitlementDate(LocalDate.now());
    }

}
