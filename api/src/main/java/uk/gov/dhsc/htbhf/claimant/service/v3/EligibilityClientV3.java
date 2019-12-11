package uk.gov.dhsc.htbhf.claimant.service.v3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.dwp.model.PersonDTO;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

/**
 * Client for calling V2 of the Eligibility Service using the RestTemplate defined in
 * {@link uk.gov.dhsc.htbhf.requestcontext.RequestContextConfiguration}.
 */
@Component
@Slf4j
public class EligibilityClientV3 {

    private static final String ELIGIBILITY_ENDPOINT = "/v2/eligibility";
    private final RestTemplate restTemplate;
    private final String eligibilityUri;

    public EligibilityClientV3(@Value("${eligibility.base-uri}") String baseUri,
                               RestTemplate restTemplate) {
        this.eligibilityUri = baseUri + ELIGIBILITY_ENDPOINT;
        this.restTemplate = restTemplate;
    }

    public CombinedIdentityAndEligibilityResponse checkIdentityAndEligibility(Claimant claimant) {
        log.debug("Checking V2 eligibility");
        PersonDTO person = buildPersonDTO(claimant);
        try {
            ResponseEntity<CombinedIdentityAndEligibilityResponse> response = restTemplate.postForEntity(
                    eligibilityUri,
                    person,
                    CombinedIdentityAndEligibilityResponse.class
            );

            if (HttpStatus.OK != response.getStatusCode()) {
                throw new EligibilityClientException(response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Exception caught trying to post to {}", eligibilityUri);
            throw new EligibilityClientException(e, eligibilityUri);
        }
    }

    private PersonDTO buildPersonDTO(Claimant claimant) {
        return PersonDTO.builder()
                .surname(claimant.getLastName())
                .nino(claimant.getNino())
                .dateOfBirth(claimant.getDateOfBirth())
                .addressLine1(claimant.getAddress().getAddressLine1())
                .postcode(claimant.getAddress().getPostcode())
                .emailAddress(claimant.getEmailAddress())
                .mobilePhoneNumber(claimant.getPhoneNumber())
                .build();
    }
}
