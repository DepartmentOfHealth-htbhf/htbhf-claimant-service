package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.util.List;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ClaimResultDTO {

    @JsonProperty("eligibilityStatus")
    @ApiModelProperty(notes = "The eligibility status of the claim")
    private EligibilityStatus eligibilityStatus;

    @JsonProperty("claimStatus")
    @ApiModelProperty(notes = "The status of the claim")
    private ClaimStatus claimStatus;

    @JsonProperty("voucherEntitlement")
    @ApiModelProperty(notes = "Details of the vouchers that the claimant is entitled to")
    private VoucherEntitlementDTO voucherEntitlement;

    @JsonProperty("claimUpdated")
    @ApiModelProperty(notes = "True if an existing, active, claim was updated. May be null if a new claim is created.")
    private Boolean claimUpdated;

    @JsonProperty("updatedFields")
    @ApiModelProperty(notes = "List of fields that were updated in the existing claim. May be null if a new claim is created.")
    private List<String> updatedFields;

    @JsonProperty("verificationResult")
    @ApiModelProperty(notes = "The result of verifying the claimant's details")
    private VerificationResult verificationResult;
}
