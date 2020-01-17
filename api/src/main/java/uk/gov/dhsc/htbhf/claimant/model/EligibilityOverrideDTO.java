package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
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

    @NotNull
    @JsonProperty("overrideUntil")
    @ApiModelProperty(notes = "The date at which the override expires", example = "2025-05-17")
    private LocalDate overrideUntil;
}