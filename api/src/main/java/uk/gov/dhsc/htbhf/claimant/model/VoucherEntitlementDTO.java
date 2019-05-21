package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the number (and value) of vouchers a claimant is entitled to.
 */
@Data
@Builder
public class VoucherEntitlementDTO {

    @JsonProperty("vouchersForChildrenUnderOne")
    @ApiModelProperty(notes = "The number of vouchers for children under one year old", example = "2")
    private int vouchersForChildrenUnderOne;

    @JsonProperty("vouchersForChildrenBetweenOneAndFour")
    @ApiModelProperty(notes = "The number of vouchers for children between one and 4 years old", example = "1")
    private int vouchersForChildrenBetweenOneAndFour;

    @JsonProperty("vouchersForPregnancy")
    @ApiModelProperty(notes = "The number of vouchers for pregnancy", example = "1")
    private int vouchersForPregnancy;

    @JsonProperty("totalVoucherEntitlement")
    @ApiModelProperty(notes = "The total number of vouchers the claimant is entitled to", example = "4")
    private int totalVoucherEntitlement;

    @JsonProperty("voucherValueInPence")
    @ApiModelProperty(notes = "The financial value of a single voucher", example = "310")
    private int voucherValueInPence;

    @JsonProperty("totalVoucherValueInPence")
    @ApiModelProperty(notes = "The sum total value of all vouchers the claimant is entitled to", example = "1240")
    private int totalVoucherValueInPence;

}
