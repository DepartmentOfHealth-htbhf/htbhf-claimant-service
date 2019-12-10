package uk.gov.dhsc.htbhf.claimant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import uk.gov.dhsc.htbhf.claimant.service.v3.EligibilityClientV3;

/**
 * Exception used when calling the Eligibility Service from the {@link EligibilityClientV3}.
 */
public class EligibilityClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EligibilityClientException(HttpStatus httpStatus) {
        super("Response code from Eligibility service was not OK, received: " + httpStatus.value());
    }

    public EligibilityClientException(HttpStatus httpStatus, Exception exception) {
        super("Response code from Eligibility service was not OK, received: " + httpStatus.value(), exception);
    }

    public EligibilityClientException(RestClientException restClientException, String endpointUrl) {
        super("Exception caught trying to call eligibility service at: " + endpointUrl, restClientException);
    }
}
