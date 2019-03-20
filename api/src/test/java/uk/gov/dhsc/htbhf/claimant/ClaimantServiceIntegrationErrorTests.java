package uk.gov.dhsc.htbhf.claimant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.controller.ErrorResponse;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;

public class ClaimantServiceIntegrationErrorTests extends AbstractIntegrationTest {

    @MockBean
    ClaimService claimService;

    @Test
    void shouldReturnInternalServiceError() {
        ClaimDTO claim = aValidClaimDTO();

        doThrow(new RuntimeException()).when(claimService).createClaim(any(Claim.class));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(endpointUrl, claim, ErrorResponse.class);

        assertErrorResponse(response, "An internal server error occurred", INTERNAL_SERVER_ERROR);
        ArgumentCaptor<Claim> claimArgumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimService).createClaim(claimArgumentCaptor.capture());
        assertThat(claimArgumentCaptor.getAllValues()).hasSize(1);
        Claim actualClaim = claimArgumentCaptor.getValue();
        assertClaimantMatchesClaimantDTO(claim.getClaimant(), actualClaim.getClaimant());
    }

}
