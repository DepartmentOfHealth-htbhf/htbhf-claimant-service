package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private int singleVoucherValueInPence;
    private int totalVoucherValueInPence;
    private LocalDate entitlementDate;

    @Builder
    public VoucherEntitlement(int vouchersForChildrenUnderOne,
                              int vouchersForChildrenBetweenOneAndFour,
                              int vouchersForPregnancy,
                              int singleVoucherValueInPence,
                              LocalDate entitlementDate) {
        this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
        this.vouchersForPregnancy = vouchersForPregnancy;
        this.singleVoucherValueInPence = singleVoucherValueInPence;
        this.entitlementDate = entitlementDate;

        this.totalVoucherEntitlement = vouchersForPregnancy + vouchersForChildrenUnderOne + vouchersForChildrenBetweenOneAndFour;
        this.totalVoucherValueInPence = totalVoucherEntitlement * singleVoucherValueInPence;
    }
}
