package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;

public class VoucherEntitlementDTOTestDataFactory {

    public static VoucherEntitlementDTO aValidVoucherEntitlementDTO() {
        return VoucherEntitlementDTO.builder()
                .voucherValue(VOUCHER_VALUE)
                .vouchersForChildrenUnderOne(VOUCHERS_FOR_CHILDREN_UNDER_ONE)
                .vouchersForChildrenBetweenOneAndFour(VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR)
                .vouchersForPregnancy(VOUCHERS_FOR_PREGNANCY)
                .totalVoucherEntitlement(TOTAL_VOUCHER_ENTITLEMENT)
                .totalVoucherValue(TOTAL_VOUCHER_VALUE)
                .build();
    }
}
