package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class representing the response from postcodes.io.
 * See http://postcodes.io/docs for api documentation.
 */
@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class PostcodeDataResponse {

    @JsonProperty("result")
    private PostcodeData postcodeData;
}
