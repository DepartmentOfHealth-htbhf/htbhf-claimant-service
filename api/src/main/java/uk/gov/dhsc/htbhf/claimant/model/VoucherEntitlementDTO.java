package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

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

    @JsonProperty("voucherValueInPence")
    private int voucherValueInPence;

    @JsonProperty("totalVoucherValueInPence")
    private int totalVoucherValueInPence;

}
