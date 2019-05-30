package uk.gov.dhsc.htbhf.claimant;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.v1.NewClaimServiceV1;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static uk.gov.dhsc.htbhf.assertions.IntegrationTestAssertions.assertInternalServerErrorResponse;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.CLAIMANT_ENDPOINT_URI;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;

/**
 * This integration test is kept separate from the other Integration Tests as it mocks out the ClaimService
 * so that it can test the scenario when an Exception is thrown from it.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class ClaimantServiceIntegrationErrorTests {

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    NewClaimServiceV1 newClaimService;

    @Test
    void shouldReturnInternalServiceError() {
        ClaimDTO claim = aValidClaimDTO();

        doThrow(new RuntimeException()).when(newClaimService).createClaim(any(Claimant.class));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(CLAIMANT_ENDPOINT_URI, claim, ErrorResponse.class);

        assertInternalServerErrorResponse(response);
        verify(newClaimService).createClaim(any(Claimant.class));
    }

}
