package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ClaimantDTO {

    @Length(max = 500)
    @JsonProperty("firstName")
    private String firstName;

    @NotNull
    @Length(min = 1, max = 500)
    @JsonProperty("secondName")
    private String secondName;
}
