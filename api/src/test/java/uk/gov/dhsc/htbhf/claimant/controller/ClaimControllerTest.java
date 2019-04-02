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
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResponseTestDataFactory.aClaimResponseWithEligibilityStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantWithStatus;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    ClaimDTOToClaimConverter converter;
    @Mock
    ClaimService claimService;

    @InjectMocks
    ClaimController controller;

    @ParameterizedTest
    @CsvSource({
            "ELIGIBLE, CREATED",
            "INELIGIBLE, OK",
            "PENDING, OK",
            "NOMATCH, NOT_FOUND",
            "DUPLICATE, OK",
            "ERROR, INTERNAL_SERVER_ERROR"
    })
    void shouldInvokeClaimServiceWithConvertedClaim(EligibilityStatus eligibilityStatus, HttpStatus httpStatus) {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        Claim convertedClaim = Claim.builder().build();
        given(converter.convert(any())).willReturn(convertedClaim);
        given(claimService.createClaim(any())).willReturn(aValidClaimantWithStatus(eligibilityStatus));

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithEligibilityStatus(eligibilityStatus);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(converter).convert(dto);
        verify(claimService).createClaim(convertedClaim);
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsInvalid() {
        // Given
        Claim convertedClaim = Claim.builder().build();
        given(converter.convert(any())).willReturn(convertedClaim);
        given(claimService.createClaim(any())).willReturn(aValidClaimantWithStatus(EligibilityStatus.ELIGIBLE));
        Map mockStatusMap = mock(Map.class);
        ReflectionTestUtils.setField(controller, "statusMap", mockStatusMap);
        given(mockStatusMap.get(EligibilityStatus.ELIGIBLE)).willReturn(null);
        ClaimDTO dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithEligibilityStatus(EligibilityStatus.ELIGIBLE);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(converter).convert(dto);
        verify(claimService).createClaim(convertedClaim);
        verify(mockStatusMap).get(EligibilityStatus.ELIGIBLE);
    }
}
