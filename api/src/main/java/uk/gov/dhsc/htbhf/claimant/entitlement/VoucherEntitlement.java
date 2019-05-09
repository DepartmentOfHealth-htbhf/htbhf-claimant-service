package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
public class VoucherEntitlement {
    private final int vouchersForChildrenUnderOne;
    private final int vouchersForChildrenBetweenOneAndFour;
    private final int vouchersForPregnancy;
    private final int totalVoucherEntitlement;
    private final int voucherValueInPence;
    private final int totalVoucherValueInPence;
    private final LocalDate entitlementDate;

    @Builder
    public VoucherEntitlement(
            int vouchersForChildrenUnderOne,
            int vouchersForChildrenBetweenOneAndFour,
            int vouchersForPregnancy,
            int voucherValueInPence,
            LocalDate entitlementDate) {
        this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
        this.vouchersForPregnancy = vouchersForPregnancy;
        this.voucherValueInPence = voucherValueInPence;
        this.entitlementDate = entitlementDate;

        this.totalVoucherEntitlement = vouchersForPregnancy + vouchersForChildrenUnderOne + vouchersForChildrenBetweenOneAndFour;
        this.totalVoucherValueInPence = totalVoucherEntitlement * voucherValueInPence;
    }
}
