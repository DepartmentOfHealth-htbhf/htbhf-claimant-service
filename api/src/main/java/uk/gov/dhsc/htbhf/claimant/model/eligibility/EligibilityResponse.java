package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class EligibilityResponse {

    @JsonProperty("eligibilityStatus")
    private EligibilityStatus eligibilityStatus;

    @JsonProperty("dwpHouseholdIdentifier")
    private String dwpHouseholdIdentifier;

    @JsonProperty("hmrcHouseholdIdentifier")
    private String hmrcHouseholdIdentifier;

    @JsonProperty("children")
    private final List<ChildDTO> children;

    public static EligibilityResponse buildWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    @JsonIgnore
    public List<LocalDate> getDateOfBirthOfChildren() {
        return children.stream()
                .map(ChildDTO::getDateOfBirth)
                .collect(toList());
    }
}
