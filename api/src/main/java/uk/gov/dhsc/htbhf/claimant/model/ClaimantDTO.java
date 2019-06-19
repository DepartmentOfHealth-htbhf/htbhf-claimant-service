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

import static uk.gov.dhsc.htbhf.claimant.model.Constants.VALID_EMAIL_REGEX;

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

    @NotNull
    @Pattern(regexp = "^\\+44\\d{9,10}$", message = "invalid UK phone number, must be in +44 format, e.g. +447123456789")
    @JsonProperty("phoneNumber")
    @ApiModelProperty(notes = "The claimant's UK phone number. Must be in +44 format, e.g. +447123456789")
    private String phoneNumber;

    @NotNull
    @Pattern(regexp = VALID_EMAIL_REGEX, message = "invalid email address")
    @Size(max = 256)
    @JsonProperty("emailAddress")
    @ApiModelProperty(notes = "The claimant's email address. e.g. test@email.com")
    private String emailAddress;

}
