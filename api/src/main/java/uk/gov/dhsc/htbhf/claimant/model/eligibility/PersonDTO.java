package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;

import java.time.LocalDate;

/**
 * Representation of what is required to  call the Eligibility Service.
 */
@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class PersonDTO {

    @JsonProperty("firstName")
    private final String firstName;

    @JsonProperty("lastName")
    private final String lastName;

    @JsonProperty("nino")
    private final String nino;

    @JsonProperty("dateOfBirth")
    private final LocalDate dateOfBirth;

    @JsonProperty("address")
    private final AddressDTOV3 address;
}
