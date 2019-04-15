package uk.gov.dhsc.htbhf.claimant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

public class CardClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CardClientException(HttpStatus httpStatus) {
        super("Response code from card service was not CREATED, received: " + httpStatus.value());
    }

    public CardClientException(RestClientException restClientException, String endpointUrl) {
        super("Exception caught trying to call card service at: " + endpointUrl, restClientException);
    }
}
