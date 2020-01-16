package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "Used to override eligibility decision from eligibility service")
public class EligibilityOverrideDTO {

    @NotNull
    @JsonProperty("eligibilityOutcome")
    @ApiModelProperty(notes = "Overrides the eligibility outcome from eligibility service")
    private EligibilityOutcome eligibilityOutcome;

}