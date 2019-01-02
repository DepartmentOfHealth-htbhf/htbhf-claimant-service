package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ClaimDTO {

    @JsonProperty("claimant")
    private ClaimantDTO claimant;
}
