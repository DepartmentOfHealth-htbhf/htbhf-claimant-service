package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Data;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to for a given payment cycle.
 */
@Data
public class PaymentCycleVoucherEntitlement {

    private final int vouchersForChildrenUnderOne;
    private final int vouchersForChildrenBetweenOneAndFour;
    private final int vouchersForPregnancy;
    private final int totalVoucherEntitlement;
    private final int voucherValueInPence;
    private final int totalVoucherValueInPence;
    private final List<VoucherEntitlement> voucherEntitlements;

    public PaymentCycleVoucherEntitlement(List<VoucherEntitlement> voucherEntitlements) {
        if (isEmpty(voucherEntitlements)) {
            throw new IllegalArgumentException("List of voucher entitlements must not be null or empty.");
        }
        this.voucherEntitlements = voucherEntitlements;

        int childrenUnderOne = 0;
        int childrenBetweenOneAndFour = 0;
        int pregnancy = 0;
        int total = 0;
        int totalValueInPence = 0;

        for (VoucherEntitlement voucherEntitlement : voucherEntitlements) {
            childrenUnderOne += voucherEntitlement.getVouchersForChildrenUnderOne();
            childrenBetweenOneAndFour += voucherEntitlement.getVouchersForChildrenBetweenOneAndFour();
            pregnancy += voucherEntitlement.getVouchersForPregnancy();
            total += voucherEntitlement.getTotalVoucherEntitlement();
            totalValueInPence += voucherEntitlement.getTotalVoucherValueInPence();
        }

        vouchersForChildrenUnderOne = childrenUnderOne;
        vouchersForChildrenBetweenOneAndFour = childrenBetweenOneAndFour;
        vouchersForPregnancy = pregnancy;
        totalVoucherEntitlement = total;
        totalVoucherValueInPence = totalValueInPence;
        voucherValueInPence = voucherEntitlements.get(0).getVoucherValueInPence();
    }
}
