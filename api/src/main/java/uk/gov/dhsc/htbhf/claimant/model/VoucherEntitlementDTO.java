package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
@Builder
public class VoucherEntitlementDTO {

    @JsonProperty("vouchersForChildrenUnderOne")
    private int vouchersForChildrenUnderOne;

    @JsonProperty("vouchersForChildrenBetweenOneAndFour")
    private int vouchersForChildrenBetweenOneAndFour;

    @JsonProperty("vouchersForPregnancy")
    private int vouchersForPregnancy;

    @JsonProperty("totalVoucherEntitlement")
    private int totalVoucherEntitlement;

    @JsonProperty("voucherValue")
    private BigDecimal voucherValue;

    @JsonProperty("totalVoucherValue")
    private BigDecimal totalVoucherValue;

}
