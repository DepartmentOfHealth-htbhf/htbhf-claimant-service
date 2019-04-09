package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
public class VoucherEntitlement {
    private final int vouchersForChildrenUnderOne;
    private final int vouchersForChildrenBetweenOneAndFour;
    private final int vouchersForPregnancy;
    private final int totalVoucherEntitlement;
    private final BigDecimal voucherValue;
    private final BigDecimal totalVoucherValue;

    @Builder
    public VoucherEntitlement(
            int vouchersForChildrenUnderOne,
            int vouchersForChildrenBetweenOneAndFour,
            int vouchersForPregnancy,
            BigDecimal voucherValue
    ) {
        this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
        this.vouchersForPregnancy = vouchersForPregnancy;
        this.voucherValue = voucherValue;

        this.totalVoucherEntitlement = vouchersForPregnancy + vouchersForChildrenUnderOne + vouchersForChildrenBetweenOneAndFour;
        this.totalVoucherValue = new BigDecimal(totalVoucherEntitlement).multiply(voucherValue);
    }
}
