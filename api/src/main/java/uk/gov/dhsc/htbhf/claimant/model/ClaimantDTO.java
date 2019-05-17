package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.model.constraint.DateWithinRelativeRange;

import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "A claimant for help to buy healthy foods.")
public class ClaimantDTO {

    @Size(max = 500)
    @JsonProperty("firstName")
    @ApiModelProperty(notes = "First (given) name", example = "Jo")
    private String firstName;

    @NotNull
    @Size(min = 1, max = 500)
    @JsonProperty("lastName")
    @ApiModelProperty(notes = "Last (family) name", example = "Bloggs")
    private String lastName;

    @NotNull
    @Pattern(regexp = "[a-zA-Z]{2}\\d{6}[a-dA-D]")
    @JsonProperty("nino")
    @ApiModelProperty(notes = "National Insurance number", example = "QQ123456C")
    private String nino;

    @NotNull
    @Past
    @JsonProperty("dateOfBirth")
    @ApiModelProperty(notes = "The date of birth, in the format YYYY-MM-DD", example = "1985-12-30")
    private LocalDate dateOfBirth;

    @JsonProperty("expectedDeliveryDate")
    @ApiModelProperty(notes = "If the claimant is pregnant, this is the expected date of delivery (due date) of their baby, in the format YYYY-MM-DD."
            + " The due date must be between one month in the past and 8 months in the future.",
            example = "2019-12-30")
    @DateWithinRelativeRange(monthsInPast = 1, monthsInFuture = 8,
            message = "must not be more than one month in the past or 8 months in the future")
    private LocalDate expectedDeliveryDate;

    @NotNull
    @Valid
    @JsonProperty("address")
    @ApiModelProperty(notes = "The address of the claimant")
    private AddressDTO address;
}
