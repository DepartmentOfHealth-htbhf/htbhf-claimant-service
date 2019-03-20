package uk.gov.dhsc.htbhf.claimant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;
import uk.gov.dhsc.htbhf.errorhandler.ErrorResponse;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.CLAIMANT_ENDPOINT_URI;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertClaimantMatchesClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.ClaimantServiceAssertionUtils.assertErrorResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ClaimantServiceIntegrationErrorTests {

    @Autowired
    TestRestTemplate restTemplate;

    @MockBean
    ClaimService claimService;

    @Test
    void shouldReturnInternalServiceError() {
        ClaimDTO claim = aValidClaimDTO();

        doThrow(new RuntimeException()).when(claimService).createClaim(any(Claim.class));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(CLAIMANT_ENDPOINT_URI, claim, ErrorResponse.class);

        assertErrorResponse(response, "An internal server error occurred", INTERNAL_SERVER_ERROR);
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimService).createClaim(claimArgumentCaptor.capture());
        assertThat(claimArgumentCaptor.getAllValues()).hasSize(1);
        Claim actualClaim = claimArgumentCaptor.getValue();
        assertClaimantMatchesClaimantDTO(claim.getClaimant(), actualClaim.getClaimant());
    }

}
