package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "List of Claims created for Apply for healthy start")
public class ClaimResponseDTO {

    @ApiModelProperty(notes = "The claim's unique id.", example = "96c3f8c0-f6d9-4ad4-9ed9-72fcbd8d692d")
    @JsonProperty("id")
    private UUID id;

    @ApiModelProperty(notes = "The claim's current status")
    @JsonProperty("claimStatus")
    private ClaimStatus claimStatus;

    @ApiModelProperty(notes = "First (given) name", example = "Jo")
    @JsonProperty("firstName")
    private String firstName;

    @ApiModelProperty(notes = "Last (surname or family) name", example = "Bloggs")
    @JsonProperty("lastName")
    private String lastName;

    @ApiModelProperty(notes = "The date of birth, in the format YYYY-MM-DD", example = "1985-12-30")
    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;

    @ApiModelProperty(notes = "First line of the address", example = "Flat B")
    @JsonProperty("addressLine1")
    private String addressLine1;

    @ApiModelProperty(notes = "The postcode of the address.", example = "AA1 1AA")
    @JsonProperty("postcode")
    private String postcode;

    @ApiModelProperty(notes = "Unique reference for claim", example = "0E1567C0B2")
    @JsonProperty("reference")
    private String reference;
}
