package uk.gov.dhsc.htbhf.claimant.entitlement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.util.Comparator.comparing;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to for a given payment cycle.
 */
@Data
@NoArgsConstructor
public class PaymentCycleVoucherEntitlement {

    private int vouchersForChildrenUnderOne;
    private int vouchersForChildrenBetweenOneAndFour;
    private int vouchersForPregnancy;
    private int totalVoucherEntitlement;
    private int singleVoucherValueInPence;
    private int totalChildrenUnderOneVoucherValueInPence;
    private int totalChildrenBetweenOneAndFourVoucherValueInPence;
    private int totalPregnancyVoucherValueInPence;
    private int totalVoucherValueInPence;
    private int backdatedVouchers;
    private List<VoucherEntitlement> voucherEntitlements;

    public PaymentCycleVoucherEntitlement(List<VoucherEntitlement> voucherEntitlements) {
        this(voucherEntitlements, 0);
    }

    @Builder
    public PaymentCycleVoucherEntitlement(List<VoucherEntitlement> voucherEntitlements, int backdatedVouchers) {
        if (isEmpty(voucherEntitlements)) {
            throw new IllegalArgumentException("List of voucher entitlements must not be null or empty.");
        }
        this.voucherEntitlements = voucherEntitlements;

        int childrenUnderOne = 0;
        int childrenBetweenOneAndFour = 0;
        int pregnancy = 0;
        int total = 0;

        for (VoucherEntitlement voucherEntitlement : voucherEntitlements) {
            childrenUnderOne += voucherEntitlement.getVouchersForChildrenUnderOne();
            childrenBetweenOneAndFour += voucherEntitlement.getVouchersForChildrenBetweenOneAndFour();
            pregnancy += voucherEntitlement.getVouchersForPregnancy();
            total += voucherEntitlement.getTotalVoucherEntitlement();
        }

        this.backdatedVouchers = backdatedVouchers;
        this.vouchersForChildrenUnderOne = childrenUnderOne;
        this.vouchersForChildrenBetweenOneAndFour = childrenBetweenOneAndFour;
        this.vouchersForPregnancy = pregnancy;
        this.totalVoucherEntitlement = total + backdatedVouchers;
        this.singleVoucherValueInPence = voucherEntitlements.get(0).getSingleVoucherValueInPence();
        this.totalChildrenUnderOneVoucherValueInPence = vouchersForChildrenUnderOne * singleVoucherValueInPence;
        this.totalChildrenBetweenOneAndFourVoucherValueInPence = vouchersForChildrenBetweenOneAndFour * singleVoucherValueInPence;
        this.totalPregnancyVoucherValueInPence = vouchersForPregnancy * singleVoucherValueInPence;
        this.totalVoucherValueInPence = totalVoucherEntitlement * singleVoucherValueInPence;
    }

    @JsonIgnore
    public VoucherEntitlement getFirstVoucherEntitlementForCycle() {
        return voucherEntitlements.stream()
                .min(comparing(VoucherEntitlement::getEntitlementDate))
                .get();
    }

    @JsonIgnore
    public VoucherEntitlement getLastVoucherEntitlementForCycle() {
        return voucherEntitlements.stream()
                .max(comparing(VoucherEntitlement::getEntitlementDate))
                .get();
    }

    @JsonIgnore
    public int getBackdatedVouchersValueInPence() {
        return singleVoucherValueInPence * backdatedVouchers;
    }
}
