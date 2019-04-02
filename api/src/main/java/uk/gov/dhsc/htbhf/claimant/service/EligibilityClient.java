package uk.gov.dhsc.htbhf.claimant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.exception.EligibilityClientException;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.NOMATCH;

/**
 * Client for calling the Eligibility Service using the RestTemplate defined in
 * {@link uk.gov.dhsc.htbhf.requestcontext.RequestContextConfiguration}.
 */
@Component
@Slf4j
public class EligibilityClient {

    public static final String ELIGIBILITY_ENDPOINT = "/v1/eligibility";
    private final RestTemplate restTemplateWithIdHeaders;
    private final ClaimantToPersonDTOConverter claimantToPersonDTOConverter;
    private final String eligibilityUri;

    public EligibilityClient(@Value("${eligibility.base-uri}") String baseUri,
                             RestTemplate restTemplateWithIdHeaders,
                             ClaimantToPersonDTOConverter claimantToPersonDTOConverter) {
        this.eligibilityUri = baseUri + ELIGIBILITY_ENDPOINT;
        this.restTemplateWithIdHeaders = restTemplateWithIdHeaders;
        this.claimantToPersonDTOConverter = claimantToPersonDTOConverter;
    }

    public EligibilityResponse checkEligibility(Claimant claimant) {
        PersonDTO person = claimantToPersonDTOConverter.convert(claimant);
        try {
            ResponseEntity<EligibilityResponse> response = restTemplateWithIdHeaders.postForEntity(
                    eligibilityUri,
                    person,
                    EligibilityResponse.class
            );

            if (HttpStatus.OK != response.getStatusCode()) {
                throw new EligibilityClientException(response.getStatusCode());
            }
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND != e.getStatusCode()) {
                log.error("Exception caught trying to post to {}", eligibilityUri);
                throw new EligibilityClientException(e.getStatusCode(), e);
            }
            return EligibilityResponse.builder().eligibilityStatus(NOMATCH).build();
        } catch (RestClientException e) {
            log.error("Exception caught trying to post to {}", eligibilityUri);
            throw new EligibilityClientException(e, eligibilityUri);
        }
    }
}
