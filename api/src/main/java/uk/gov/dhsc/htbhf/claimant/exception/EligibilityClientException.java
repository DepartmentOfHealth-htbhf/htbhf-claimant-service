package uk.gov.dhsc.htbhf.claimant.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception used when calling the Eligibility Service from the {@link uk.gov.dhsc.htbhf.claimant.service.EligibilityClient}.
 */
public class EligibilityClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EligibilityClientException(HttpStatus httpStatus) {
        super("Response code from Eligibility service was not OK, received: " + httpStatus.value());
    }
}
