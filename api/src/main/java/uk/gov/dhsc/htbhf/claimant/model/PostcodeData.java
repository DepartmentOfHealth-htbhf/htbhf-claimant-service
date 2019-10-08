package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


/**
 * Class representing postcode data as returned from postcodes.io.
 * See http://postcodes.io/docs for api documentation.
 */
@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@SuppressWarnings("PMD.TooManyFields")
public class PostcodeData {

    @JsonProperty("postcode")
    private String postcode;

    @JsonProperty("quality")
    private Integer quality;

    @JsonProperty("northings")
    private Integer northings;

    @JsonProperty("country")
    private String country;

    @JsonProperty("nhs_ha")
    private String nhsHa;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("european_electoral_region")
    private String europeanElectoralRegion;

    @JsonProperty("primary_care_trust")
    private String primaryCareTrust;

    @JsonProperty("region")
    private String region;

    @JsonProperty("lsoa")
    private String lsoa;

    @JsonProperty("msoa")
    private String msoa;

    @JsonProperty("incode")
    private String incode;

    @JsonProperty("outcode")
    private String outcode;

    @JsonProperty("parliamentary_constituency")
    private String parliamentaryConstituency;

    @JsonProperty("admin_district")
    private String adminDistrict;

    @JsonProperty("parish")
    private String parish;

    @JsonProperty("admin_county")
    private String adminCounty;

    @JsonProperty("admin_ward")
    private String adminWard;

    @JsonProperty("ced")
    private String ced;

    @JsonProperty("ccg")
    private String ccg;

    @JsonProperty("nuts")
    private String nuts;

    @JsonProperty("codes")
    private PostcodeDataCodes codes;
}
