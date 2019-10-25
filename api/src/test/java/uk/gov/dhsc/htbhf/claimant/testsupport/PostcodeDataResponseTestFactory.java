package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;

import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;

public class PostcodeDataResponseTestFactory {

    public static PostcodeDataResponse aPostcodeDataResponseObjectForPostcode(String postcode) {
        PostcodeData postcodeData = aPostcodeDataObjectForPostcode(postcode);
        return new PostcodeDataResponse(postcodeData);
    }
}
