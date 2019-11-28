package uk.gov.dhsc.htbhf.claimant.creator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class ClaimantInfo {

    @JsonProperty("firstName")
    String firstName;
    @JsonProperty("lastName")
    String lastName;
    @JsonProperty("nino")
    String nino;
    @JsonProperty("dateOfBirth")
    LocalDate dateOfBirth;
    @JsonProperty("mobile")
    String mobile;
    @JsonProperty("emailAddress")
    String emailAddress;
    @JsonProperty("expectedDeliveryDate")
    LocalDate expectedDeliveryDate;
    @JsonProperty("address")
    AddressDTO addressDTO;
    @JsonProperty("childrenInfo")
    List<ChildInfo> childrenInfo;
}
