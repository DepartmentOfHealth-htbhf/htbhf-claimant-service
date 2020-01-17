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
import uk.gov.dhsc.htbhf.claimant.converter.NewClaimDTOToClaimRequestConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.NewClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimRequestTestDataFactory.aValidClaimRequest;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultTestDataFactory.aClaimResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTOWithEligibilityOverride;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    ClaimService claimService;

    @Mock
    VoucherEntitlementToDTOConverter entitlementConverter;

    @Mock
    NewClaimDTOToClaimRequestConverter claimRequestConverter;

    @InjectMocks
    ClaimController controller;

    @ParameterizedTest
    @CsvSource({
            "NEW, CREATED",
            "PENDING, OK",
            "ACTIVE, OK",
            "PENDING_EXPIRY, OK"
    })
    void shouldInvokeClaimServiceAndReturnCorrectStatusWithVoucherEntitlement(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        NewClaimDTO dto = aValidClaimDTO();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.of(aValidVoucherEntitlement()));
        ClaimRequest claimRequest = aValidClaimRequest();
        VoucherEntitlementDTO entitlementDTO = aValidVoucherEntitlementDTO();
        given(claimRequestConverter.convert(any())).willReturn(claimRequest);
        given(entitlementConverter.convert(any())).willReturn(entitlementDTO);
        given(claimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatus(claimStatus));
        verify(claimRequestConverter).convert(dto);
        verify(claimService).createClaim(claimRequest);
        verify(entitlementConverter).convert(claimResult.getVoucherEntitlement().get());
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, CREATED",
            "PENDING, OK",
            "ACTIVE, OK",
            "PENDING_EXPIRY, OK"
    })
    void shouldInvokeClaimServiceAndReturnCorrectStatusWithEligibilityOverride(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        NewClaimDTO dto = aValidClaimDTOWithEligibilityOverride(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                NO_CHILDREN,
                EligibilityOutcome.CONFIRMED,
                OVERRIDE_UNTIL_FIVE_YEARS);
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.of(aValidVoucherEntitlement()));
        VoucherEntitlementDTO entitlementDTO = aValidVoucherEntitlementDTO();
        ClaimRequest claimRequest = aValidClaimRequest();
        given(claimRequestConverter.convert(any())).willReturn(claimRequest);
        given(entitlementConverter.convert(any())).willReturn(entitlementDTO);
        given(claimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatus(claimStatus));
        verify(claimRequestConverter).convert(dto);
        verify(claimService).createClaim(claimRequest);
        verify(entitlementConverter).convert(claimResult.getVoucherEntitlement().get());
    }

    @ParameterizedTest
    @CsvSource({
            "REJECTED, OK",
            "EXPIRED, OK",
            "ERROR, INTERNAL_SERVER_ERROR"
    })
    void shouldInvokeClaimServiceAndReturnCorrectStatusWithoutVoucherEntitlement(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        NewClaimDTO dto = aValidClaimDTO();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.empty());
        ClaimRequest claimRequest = aValidClaimRequest();
        given(claimRequestConverter.convert(any())).willReturn(claimRequest);
        given(claimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(claimStatus));
        verify(claimRequestConverter).convert(dto);
        verify(claimService).createClaim(claimRequest);
        verifyNoInteractions(entitlementConverter);
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsError() {
        // Given
        given(claimService.createClaim(any())).willReturn(aClaimResult(ClaimStatus.ERROR, Optional.empty()));
        ClaimRequest claimRequest = aValidClaimRequest();
        given(claimRequestConverter.convert(any())).willReturn(claimRequest);
        NewClaimDTO dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(ClaimStatus.ERROR));
        verify(claimService).createClaim(claimRequest);
        verify(claimRequestConverter).convert(dto);
    }

}
