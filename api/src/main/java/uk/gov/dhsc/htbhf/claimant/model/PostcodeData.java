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

    private static final String NOT_FOUND_STRING = "NOT_FOUND";

    @JsonProperty("postcode")
    private String postcode;

    @JsonProperty("quality")
    private Integer quality;

    @JsonProperty("eastings")
    private Integer eastings;

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

    public static final PostcodeData NOT_FOUND = PostcodeData.builder()
            .adminWard(NOT_FOUND_STRING)
            .adminCounty(NOT_FOUND_STRING)
            .adminDistrict(NOT_FOUND_STRING)
            .parliamentaryConstituency(NOT_FOUND_STRING)
            .postcode(NOT_FOUND_STRING)
            .ccg(NOT_FOUND_STRING)
            .ced(NOT_FOUND_STRING)
            .country(NOT_FOUND_STRING)
            .europeanElectoralRegion(NOT_FOUND_STRING)
            .incode(NOT_FOUND_STRING)
            .outcode(NOT_FOUND_STRING)
            .lsoa(NOT_FOUND_STRING)
            .msoa(NOT_FOUND_STRING)
            .nhsHa(NOT_FOUND_STRING)
            .nuts(NOT_FOUND_STRING)
            .parish(NOT_FOUND_STRING)
            .primaryCareTrust(NOT_FOUND_STRING)
            .region(NOT_FOUND_STRING)
            .codes(PostcodeDataCodes.builder()
                    .parliamentaryConstituency(NOT_FOUND_STRING)
                    .adminWard(NOT_FOUND_STRING)
                    .adminCounty(NOT_FOUND_STRING)
                    .adminDistrict(NOT_FOUND_STRING)
                    .ccg(NOT_FOUND_STRING)
                    .ced(NOT_FOUND_STRING)
                    .nuts(NOT_FOUND_STRING)
                    .parish(NOT_FOUND_STRING)
                    .build())
            .build();
}
