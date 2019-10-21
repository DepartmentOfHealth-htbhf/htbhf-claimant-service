package uk.gov.dhsc.htbhf.claimant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

/**
 * Exception used when calling google analytics from the {@link uk.gov.dhsc.htbhf.claimant.reporting.GoogleAnalyticsClient}.
 */
public class GoogleAnalyticsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GoogleAnalyticsException(HttpStatus httpStatus) {
        super("Response code from google analytics was not as expected, received: " + httpStatus.name());
    }

    public GoogleAnalyticsException(RestClientException restClientException, String endpointUrl) {
        super("Exception caught trying to call google analytics at: " + endpointUrl, restClientException);
    }
}
