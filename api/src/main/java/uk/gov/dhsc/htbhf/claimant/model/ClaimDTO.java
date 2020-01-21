package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import uk.gov.dhsc.htbhf.claimant.entity.CardStatus;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "An existing claim for Apply for healthy start.")
public class ClaimDTO {

    @ApiModelProperty(notes = "The claim's unique id.", example = "96c3f8c0-f6d9-4ad4-9ed9-72fcbd8d692d")
    @JsonProperty("id")
    private UUID id;

    @ApiModelProperty(notes = "The claim's current status")
    @JsonProperty("claimStatus")
    private ClaimStatus claimStatus;

    @ApiModelProperty(notes = "Timestamp of when the claim status was last updated", example = "2020/01/06T14:00:00.352")
    @JsonProperty("claimStatusTimestamp")
    private LocalDateTime claimStatusTimestamp;

    @ApiModelProperty(notes = "The claim's eligibility status")
    @JsonProperty("eligibilityStatus")
    private EligibilityStatus eligibilityStatus;

    @ApiModelProperty(notes = "Timestamp of when the eligibility status was last updated", example = "2020/01/06T14:00:00.352")
    @JsonProperty("eligibilityStatusTimestamp")
    private LocalDateTime eligibilityStatusTimestamp;

    @ApiModelProperty(notes = "Household identifier returned from DWP", example = "dd77e887-49cf-43cd-b974-b17273602018")
    @JsonProperty("dwpHouseholdIdentifier")
    private String dwpHouseholdIdentifier;

    @ApiModelProperty(notes = "Household identifier returned from HMRC", example = "4c697b0a-5709-423b-8e31-b63796cdca7f")
    @JsonProperty("hmrcHouseholdIdentifier")
    private String hmrcHouseholdIdentifier;

    @ApiModelProperty(notes = "Card id returned from the card provider", example = "8b66a86f-778e-47f9-b998-a042476ef2c0")
    @JsonProperty("cardAccountId")
    private String cardAccountId;

    @ApiModelProperty(notes = "The current card status")
    @JsonProperty("cardStatus")
    private CardStatus cardStatus;

    @ApiModelProperty(notes = "Time of when the card status was last updated", example = "2020/01/06T14:00:00.352")
    @JsonProperty("cardStatusTimestamp")
    private LocalDateTime cardStatusTimestamp;

    @ApiModelProperty(notes = "The initial identity and eligibility response that was performed when the claimant first applied.")
    @JsonProperty("initialIdentityAndEligibilityResponse")
    private CombinedIdentityAndEligibilityResponse initialIdentityAndEligibilityResponse;

    @ApiModelProperty(notes = "The current identity and eligibility response when processing the last payment cycle.")
    @JsonProperty("currentIdentityAndEligibilityResponse")
    private CombinedIdentityAndEligibilityResponse currentIdentityAndEligibilityResponse;

    @JsonProperty("claimant")
    private ClaimantDTO claimant;

    @ApiModelProperty(notes = "Eligibility override decision based on eligibility outcome and until date")
    @JsonProperty("eligibilityOverride")
    private EligibilityOverrideDTO eligibilityOverride;
}
