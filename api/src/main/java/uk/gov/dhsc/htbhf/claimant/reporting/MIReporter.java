package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataResponse;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@Slf4j
public class MIReporter {

    private static final String POSTCODES_PATH = "/postcodes/";

    private final RestTemplate restTemplate;
    private final ClaimRepository claimRepository;
    private final String postcodesUri;

    public MIReporter(@Value("${postcodes-io.base-uri}") String postcodesBaseUri,
                      RestTemplate restTemplate,
                      ClaimRepository claimRepository) {
        this.restTemplate = restTemplate;
        this.claimRepository = claimRepository;
        this.postcodesUri = postcodesBaseUri + POSTCODES_PATH;
    }

    public void reportClaim(Claim claim) {
        if (claim.getPostcodeData() == null) {
            PostcodeData postcodeData = getPostcodeData(claim);
            claim.setPostcodeData(postcodeData);
            claimRepository.save(claim);
        }
    }

    private PostcodeData getPostcodeData(Claim claim) {
        String postcode = claim.getClaimant().getAddress().getPostcode().replace(" ", "");

        try {
            PostcodeDataResponse response = restTemplate.getForObject(postcodesUri + postcode, PostcodeDataResponse.class);
            return getPostcodeDataFromResponse(response, claim, postcode);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == NOT_FOUND) {
                log.error("No postcode data found for postcode {} on claim {}", postcode, claim.getId());
                return PostcodeData.NOT_FOUND;
            }

            throw e;
        }
    }

    private PostcodeData getPostcodeDataFromResponse(PostcodeDataResponse response, Claim claim, String postcode) {
        if (response == null || response.getPostcodeData() == null) {
            String errorMessage = "Received null response from postcodes.io for postcode " + postcode + " on claim " + claim.getId();
            log.error(errorMessage);
            throw new NullPointerException(errorMessage);
        }

        return response.getPostcodeData();
    }
}
