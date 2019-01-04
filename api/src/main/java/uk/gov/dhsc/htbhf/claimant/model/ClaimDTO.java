package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "A claim (application) for help to buy healthy foods. Contains all details that need to be persisted as part of the application.")
public class ClaimDTO {

    @JsonProperty("claimant")
    @Valid
    @NotNull
    @ApiModelProperty(notes = "The person making the claim")
    private ClaimantDTO claimant;
}
