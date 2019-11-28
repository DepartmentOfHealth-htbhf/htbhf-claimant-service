package uk.gov.dhsc.htbhf.claimant.model.v3;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "A claim (application) for help to buy healthy foods. Contains all details that need to be persisted as part of the application.")
public class ClaimDTOV3 {

    @JsonProperty("claimant")
    @Valid
    @NotNull
    @ApiModelProperty(notes = "The person making the claim")
    private ClaimantDTOV3 claimant;

    @JsonProperty("deviceFingerprint")
    @ApiModelProperty(notes = "The fingerprint of the device used to make the claim, as best it can be identified. Probably a collection of header values.")
    private Map<String, Object> deviceFingerprint;

    @JsonProperty("webUIVersion")
    @ApiModelProperty(notes = "The version of the web-ui application used to create this request. "
            + "Identifies which version of the Terms and Conditions the applicant accepted.")
    private String webUIVersion;

}
