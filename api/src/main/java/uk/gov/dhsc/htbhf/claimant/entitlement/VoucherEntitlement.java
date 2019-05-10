package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
@NoArgsConstructor
public class VoucherEntitlement {
    private int vouchersForChildrenUnderOne;
    private int vouchersForChildrenBetweenOneAndFour;
    private int vouchersForPregnancy;
    private int totalVoucherEntitlement;
    private int voucherValueInPence;
    private int totalVoucherValueInPence;

    @Builder
    public VoucherEntitlement(
            int vouchersForChildrenUnderOne,
            int vouchersForChildrenBetweenOneAndFour,
            int vouchersForPregnancy,
            int voucherValueInPence
    ) {
        this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
        this.vouchersForPregnancy = vouchersForPregnancy;
        this.voucherValueInPence = voucherValueInPence;

        this.totalVoucherEntitlement = vouchersForPregnancy + vouchersForChildrenUnderOne + vouchersForChildrenBetweenOneAndFour;
        this.totalVoucherValueInPence = totalVoucherEntitlement * voucherValueInPence;
    }
}
