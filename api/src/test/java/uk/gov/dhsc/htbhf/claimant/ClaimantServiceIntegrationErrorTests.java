package uk.gov.dhsc.htbhf.claimant;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimDTOV3;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertInternalServerErrorResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aValidClaimDTO;

/**
 * This integration test is kept separate from the other Integration Tests as it mocks out the ClaimService
 * so that it can test the scenario when an Exception is thrown from it.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
@AutoConfigureWireMock(port = 8100)
class ClaimantServiceIntegrationErrorTests {

    private static final URI CLAIMANT_ENDPOINT_URI_V3 = URI.create("/v3/claims");

    @Autowired
    TestRestTemplate restTemplate;

    @AfterEach
    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void shouldReturnInternalServiceError() {
        ClaimDTOV3 claim = aValidClaimDTO();

        stubFor(post(urlEqualTo("/v2/eligibility")).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(CLAIMANT_ENDPOINT_URI_V3, claim, ErrorResponse.class);

        assertInternalServerErrorResponse(response);
    }

}
