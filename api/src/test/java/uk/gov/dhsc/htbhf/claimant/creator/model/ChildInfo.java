package uk.gov.dhsc.htbhf.claimant.creator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Period;

@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ChildInfo {

    @JsonProperty("age")
    Period age;
    @JsonProperty("at")
    AgeAt at;
    @JsonProperty("excludeFromExistingCycle")
    boolean excludeFromExistingCycle;
}
