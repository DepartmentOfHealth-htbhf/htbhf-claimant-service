package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;

public class VoucherEntitlementDTOTestDataFactory {

    public static VoucherEntitlementDTO aValidVoucherEntitlementDTO() {
        return VoucherEntitlementDTO.builder()
                .singleVoucherValueInPence(VOUCHER_VALUE_IN_PENCE)
                .vouchersForChildrenUnderOne(VOUCHERS_FOR_CHILDREN_UNDER_ONE)
                .vouchersForChildrenBetweenOneAndFour(VOUCHERS_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR)
                .vouchersForPregnancy(VOUCHERS_FOR_PREGNANCY)
                .totalVoucherEntitlement(TOTAL_VOUCHER_ENTITLEMENT)
                .totalVoucherValueInPence(TOTAL_VOUCHER_VALUE_IN_PENCE)
                .build();
    }
}
