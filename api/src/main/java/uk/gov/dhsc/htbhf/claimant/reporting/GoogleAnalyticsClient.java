package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.claimant.exception.GoogleAnalyticsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * Service for interacting with google analytics measurement protocol.
 * See https://developers.google.com/analytics/devguides/collection/protocol/v1/
 */
@Component
@Slf4j
public class GoogleAnalyticsClient {

    private static final String REPORT_ENDPOINT = "/collect";

    private final URI googleAnalyticsUri;
    private final RestTemplate restTemplate;

    public GoogleAnalyticsClient(@Value("${google-analytics.base-uri}") String baseUri,
                                 RestTemplate restTemplate) throws URISyntaxException {
        this.googleAnalyticsUri = new URI(baseUri + REPORT_ENDPOINT);
        this.restTemplate = restTemplate;
    }

    public void reportEvent(Map<String, String> reportProperties) {
        String payload = createQueryStringWithUrlEncodedValues(reportProperties);

        HttpHeaders headers = new HttpHeaders();
        // Google analytics requires plain text for the content type.
        headers.setContentType(TEXT_PLAIN);
        RequestEntity<String> requestEntity = new RequestEntity<>(payload, headers, POST, googleAnalyticsUri);

        try {
            ResponseEntity<Void> responseEntity = restTemplate.exchange(requestEntity, Void.class);
            checkResponseIsOk(responseEntity.getStatusCode());
        } catch (RestClientException e) {
            log.error("Exception caught trying to post to {}", googleAnalyticsUri.toString());
            throw new GoogleAnalyticsException(e, googleAnalyticsUri.toString());
        }
    }

    private String createQueryStringWithUrlEncodedValues(Map<String, String> reportProperties) {
        return reportProperties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), UTF_8))
                .collect(Collectors.joining("&"));
    }

    private void checkResponseIsOk(HttpStatus status) {
        if (status != OK) {
            log.error("Expecting response status from google analytics to be OK, instead received {}", status);
            throw new GoogleAnalyticsException(status);
        }
    }
}
