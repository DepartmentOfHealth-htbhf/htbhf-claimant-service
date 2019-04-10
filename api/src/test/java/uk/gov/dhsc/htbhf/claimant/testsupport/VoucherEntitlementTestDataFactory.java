package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;

import java.math.BigDecimal;

public class VoucherEntitlementTestDataFactory {

    public static VoucherEntitlement aValidVoucherEntitlement() {
        return VoucherEntitlement.builder()
                .voucherValue(new BigDecimal("3.10"))
                .vouchersForChildrenUnderOne(2)
                .vouchersForChildrenBetweenOneAndFour(1)
                .vouchersForPregnancy(1)
                .build();
    }
}
