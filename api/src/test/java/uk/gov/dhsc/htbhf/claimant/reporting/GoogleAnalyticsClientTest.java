package uk.gov.dhsc.htbhf.claimant.reporting;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.dhsc.htbhf.claimant.exception.GoogleAnalyticsException;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8150)
class GoogleAnalyticsClientTest {

    private static final String REPORT_ENDPOINT = "/collect";
    // properties need to be ordered for the assertion against the encoded string to work
    private static final Map<String, String> REPORT_PROPERTIES = new LinkedHashMap<>();
    // google analytics expects the payload to be url encoded and in the format of a query string
    private static final String URL_ENCODED_REPORT_PROPERTIES = "cd8=Bristol+West&cd9=NHS+Bristol%2C+North+Somerset+and+South+Gloucestershire";

    static {
        REPORT_PROPERTIES.put("cd8", "Bristol West");
        REPORT_PROPERTIES.put("cd9", "NHS Bristol, North Somerset and South Gloucestershire");
    }

    @Value("${google-analytics.base-uri}")
    private String baseUri;

    @Autowired
    private GoogleAnalyticsClient googleAnalyticsClient;

    @AfterEach
    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void shouldCallGoogleAnalyticsWithUrlEncodedQueryStringPayload() {
        stubFor(post(urlEqualTo(REPORT_ENDPOINT)).withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .willReturn(ok()));

        googleAnalyticsClient.reportEvent(REPORT_PROPERTIES);

        verify(exactly(1), postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(equalTo(URL_ENCODED_REPORT_PROPERTIES)));
    }

    @Test
    void shouldThrowExceptionWhenGoogleAnalyticsReturnsANonOkResponseCode() {
        stubFor(post(urlEqualTo(REPORT_ENDPOINT)).withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .willReturn((noContent())));

        GoogleAnalyticsException exception = catchThrowableOfType(() -> googleAnalyticsClient.reportEvent(REPORT_PROPERTIES), GoogleAnalyticsException.class);

        assertThat(exception.getMessage()).isEqualTo("Response code from google analytics was not as expected, received: NO_CONTENT");
        verify(exactly(1), postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(equalTo(URL_ENCODED_REPORT_PROPERTIES)));
    }

    @Test
    void shouldThrowExceptionWhenGoogleAnalyticsReturnsAnErrorResponse() {
        stubFor(post(urlEqualTo(REPORT_ENDPOINT)).withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .willReturn((serverError())));

        GoogleAnalyticsException exception = catchThrowableOfType(() -> googleAnalyticsClient.reportEvent(REPORT_PROPERTIES), GoogleAnalyticsException.class);

        assertThat(exception.getMessage()).isEqualTo("Exception caught trying to call google analytics at: " + baseUri + REPORT_ENDPOINT);
        assertThat(exception.getCause()).isInstanceOf(HttpServerErrorException.class);
        verify(exactly(1), postRequestedFor(urlEqualTo(REPORT_ENDPOINT))
                .withHeader("Content-Type", equalTo(TEXT_PLAIN_VALUE))
                .withRequestBody(equalTo(URL_ENCODED_REPORT_PROPERTIES)));
    }

}
