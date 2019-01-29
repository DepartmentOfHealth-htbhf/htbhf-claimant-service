package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@ApiModel(description = "An address for help to buy healthy foods.")
public class AddressDTO {

    /**
     * Regex for matching UK postcodes matching BS7666 format.
     * { @see https://www.gov.uk/government/publications/bulk-data-transfer-for-sponsors-xml-schema } The format is in the file BulkDataCommon-v2.1.xsd
     * { @see https://stackoverflow.com/questions/164979/uk-postcode-regex-comprehensive }
     */
    public static final String UK_POST_CODE_REGEX = "([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})"
            + "|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9][A-Za-z]?))))\\s?[0-9][A-Za-z]{2})";

    @NotNull
    @Size(min = 1, max = 500)
    @JsonProperty("addressLine1")
    @ApiModelProperty(notes = "First line of the card delivery address", example = "Flat B")
    private String addressLine1;

    @Size(max = 500)
    @JsonProperty("addressLine2")
    @ApiModelProperty(notes = "Second line of the card delivery address", example = "123 Fake Street")
    private String addressLine2;

    @NotNull
    @Size(min = 1, max = 500)
    @JsonProperty("townOrCity")
    @ApiModelProperty(notes = "Town or city of the card delivery address", example = "London")
    private String townOrCity;

    @NotNull
    @Pattern(regexp = UK_POST_CODE_REGEX, message = "invalid postcode format")
    @JsonProperty("postcode")
    @ApiModelProperty(notes = "The postcode to send the address to.", example = "AA1 1AA")
    private String postcode;
}
