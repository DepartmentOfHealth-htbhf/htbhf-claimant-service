package uk.gov.dhsc.htbhf.claimant.entitlement;

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

    public static VoucherEntitlementBuilder builder() {
        return new VoucherEntitlementBuilder();
    }

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static class VoucherEntitlementBuilder {
        private int vouchersForChildrenUnderOne;
        private int vouchersForChildrenBetweenOneAndFour;
        private int vouchersForPregnancy;
        private BigDecimal voucherValue;

        public VoucherEntitlementBuilder vouchersForChildrenUnderOne(int vouchersForChildrenUnderOne) {
            this.vouchersForChildrenUnderOne = vouchersForChildrenUnderOne;
            return this;
        }

        public VoucherEntitlementBuilder vouchersForChildrenBetweenOneAndFour(int vouchersForChildrenBetweenOneAndFour) {
            this.vouchersForChildrenBetweenOneAndFour = vouchersForChildrenBetweenOneAndFour;
            return this;
        }

        public VoucherEntitlementBuilder vouchersForPregnancy(int vouchersForPregnancy) {
            this.vouchersForPregnancy = vouchersForPregnancy;
            return this;
        }

        public VoucherEntitlementBuilder voucherValue(BigDecimal voucherValue) {
            this.voucherValue = voucherValue;
            return this;
        }

        public VoucherEntitlement build() {
            return new VoucherEntitlement(vouchersForChildrenUnderOne, vouchersForChildrenBetweenOneAndFour, vouchersForPregnancy, voucherValue);
        }
    }
}
