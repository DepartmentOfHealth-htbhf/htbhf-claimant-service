package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ChildDTO {

    @JsonProperty("dateOfBirth")
    private final LocalDate dateOfBirth;
}
