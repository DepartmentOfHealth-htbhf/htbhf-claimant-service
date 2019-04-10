package uk.gov.dhsc.htbhf.claimant.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResponseTestDataFactory.aClaimResponseWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantWithClaimStatus;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    ClaimService claimService;

    @InjectMocks
    ClaimController controller;

    @ParameterizedTest
    @CsvSource({
            "REJECTED, OK",
            "NEW, CREATED",
            "PENDING, OK",
            "ACTIVE, OK",
            "PENDING_EXPIRY, OK",
            "EXPIRED, OK",
            "ERROR, INTERNAL_SERVER_ERROR"
    })
    void shouldInvokeClaimServiceWithConvertedClaim(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        given(claimService.createClaim(any())).willReturn(Claim.builder().claimant(aValidClaimantWithClaimStatus(claimStatus)).build());

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithClaimStatus(claimStatus);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(claimService).createClaim(dto);
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsInvalid() {
        // Given
        given(claimService.createClaim(any())).willReturn(Claim.builder().claimant(aValidClaimantWithClaimStatus(ClaimStatus.NEW)).build());
        Map mockStatusMap = mock(Map.class);
        ReflectionTestUtils.setField(controller, "statusMap", mockStatusMap);
        given(mockStatusMap.get(ClaimStatus.NEW)).willReturn(null);
        ClaimDTO dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithClaimStatus(ClaimStatus.NEW);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(claimService).createClaim(dto);
        verify(mockStatusMap).get(ClaimStatus.NEW);
    }
}
