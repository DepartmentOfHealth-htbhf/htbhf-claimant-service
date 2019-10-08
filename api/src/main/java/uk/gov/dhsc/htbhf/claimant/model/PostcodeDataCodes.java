package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Class representing a 'codes' object as returned from postcodes.io.
 * See http://postcodes.io/docs for api documentation.
 */
@Data
@Builder
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class PostcodeDataCodes {

    @JsonProperty("admin_district")
    private String adminDistrict;

    @JsonProperty("admin_county")
    private String adminCounty;

    @JsonProperty("admin_ward")
    private String adminWard;

    @JsonProperty("parish")
    private String parish;

    @JsonProperty("parliamentary_constituency")
    private String parliamentaryConstituency;

    @JsonProperty("ccg")
    private String ccg;

    @JsonProperty("ced")
    private String ced;

    @JsonProperty("nuts")
    private String nuts;
}
