package uk.gov.dhsc.htbhf.claimant.controller.v3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimantDTOToClaimantConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimDTOV3;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultTestDataFactory.aClaimResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class ClaimControllerV3Test {

    @Mock
    ClaimService claimService;

    @Mock
    ClaimantDTOToClaimantConverter claimantConverter;

    @Mock
    VoucherEntitlementToDTOConverter entitlementConverter;

    @InjectMocks
    ClaimControllerV3 controller;

    //Single unit test created to make sure the conversion from V3 to V2 objects works correctly, if any of the fields
    // are not mapped correctly in the code in ClaimControllerV3 then this test will fail.
    @Test
    void shouldInvokeClaimServiceWithConvertedClaimWithVoucherEntitlement() {
        // Given
        ClaimDTOV3 dto = aValidClaimDTO();
        Claimant claimant = aValidClaimant();
        ClaimResult claimResult = aClaimResult(ClaimStatus.NEW, Optional.of(aValidVoucherEntitlement()));
        VoucherEntitlementDTO entitlementDTO = aValidVoucherEntitlementDTO();
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(entitlementConverter.convert(any())).willReturn(entitlementDTO);
        given(claimService.createOrUpdateClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createOrUpdateClaimV3(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatus(ClaimStatus.NEW));
        verifyCreateOrUpdateClaimCalledCorrectly(claimant, dto);
        verify(claimantConverter).convert(ClaimantDTOTestDataFactory.aValidClaimantDTO());
        verify(entitlementConverter).convert(claimResult.getVoucherEntitlement().get());
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsError() {
        // Given
        given(claimService.createOrUpdateClaim(any())).willReturn(aClaimResult(ClaimStatus.ERROR, Optional.empty()));
        Claimant claimant = aValidClaimant();
        given(claimantConverter.convert(any())).willReturn(claimant);
        ClaimDTOV3 dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createOrUpdateClaimV3(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(ClaimStatus.ERROR));
        verifyCreateOrUpdateClaimCalledCorrectly(claimant, dto);
        verify(claimantConverter).convert(ClaimantDTOTestDataFactory.aValidClaimantDTO());
    }

    private void verifyCreateOrUpdateClaimCalledCorrectly(Claimant claimant, ClaimDTOV3 dto) {
        ArgumentCaptor<ClaimRequest> captor = ArgumentCaptor.forClass(ClaimRequest.class);
        verify(claimService).createOrUpdateClaim(captor.capture());
        ClaimRequest claimRequest = captor.getValue();
        assertThat(claimRequest).isNotNull();
        assertThat(claimRequest.getClaimant()).isEqualTo(claimant);
        assertThat(claimRequest.getDeviceFingerprint()).isEqualTo(dto.getDeviceFingerprint());
        assertThat(claimRequest.getWebUIVersion()).isEqualTo(dto.getWebUIVersion());
    }

}
