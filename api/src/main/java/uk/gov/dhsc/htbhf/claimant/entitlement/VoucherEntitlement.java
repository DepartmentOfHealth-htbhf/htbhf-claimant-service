package uk.gov.dhsc.htbhf.claimant.entitlement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
@Builder
public class VoucherEntitlement {
    private int vouchersForChildrenUnderOne;
    private int vouchersForChildrenBetweenOneAndFour;
    private int vouchersForPregnancy;
    private int totalVoucherEntitlement;
    private BigDecimal voucherValue;
    private BigDecimal totalVoucherValue;

}
