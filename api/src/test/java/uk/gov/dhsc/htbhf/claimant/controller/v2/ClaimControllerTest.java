package uk.gov.dhsc.htbhf.claimant.controller.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimantDTOToClaimantConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResultDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimService;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultDTOTestDataFactory.aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultTestDataFactory.aClaimResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResultTestDataFactory.anUpdatedClaimResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VerificationResultTestDataFactory.anAllMatchedVerificationResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    ClaimService claimService;

    @Mock
    ClaimantDTOToClaimantConverter claimantConverter;

    @Mock
    VoucherEntitlementToDTOConverter entitlementConverter;

    @InjectMocks
    ClaimController controller;

    @ParameterizedTest
    @CsvSource({
            "NEW, CREATED",
            "PENDING, OK",
            "ACTIVE, OK",
            "PENDING_EXPIRY, OK"
    })
    void shouldInvokeClaimServiceWithConvertedClaimWithVoucherEntitlement(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimant();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.of(aValidVoucherEntitlement()));
        VoucherEntitlementDTO entitlementDTO = aValidVoucherEntitlementDTO();
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(entitlementConverter.convert(any())).willReturn(entitlementDTO);
        given(claimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatus(claimStatus));
        verifyCreateOrUpdateClaimCalledCorrectly(claimant, dto);
        verify(claimantConverter).convert(dto.getClaimant());
        verify(entitlementConverter).convert(claimResult.getVoucherEntitlement().get());
    }

    @ParameterizedTest
    @CsvSource({
            "REJECTED, OK",
            "EXPIRED, OK",
            "ERROR, INTERNAL_SERVER_ERROR"
    })
    void shouldInvokeClaimServiceWithConvertedClaimWithoutVoucherEntitlement(ClaimStatus claimStatus, HttpStatus httpStatus) {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimant();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.empty());
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(claimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(claimStatus));
        verifyCreateOrUpdateClaimCalledCorrectly(claimant, dto);
        verify(claimantConverter).convert(dto.getClaimant());
        verifyNoInteractions(entitlementConverter);
    }

    @Test
    void shouldReturnOkStatusAndUpdatedFieldsWhenClaimHasBeenUpdated() {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        Claimant claimant = aValidClaimant();
        String updatedField = EXPECTED_DELIVERY_DATE.getFieldName();
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(claimService.createClaim(any())).willReturn(anUpdatedClaimResult(updatedField));

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(OK);
        ClaimResultDTO claimResult = response.getBody();
        assertThat(claimResult).isNotNull();
        assertThat(claimResult.getClaimUpdated()).isTrue();
        assertThat(claimResult.getUpdatedFields()).isEqualTo(singletonList(updatedField));
        assertThat(claimResult.getVerificationResult()).isEqualTo(anAllMatchedVerificationResult());
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsInvalid() {
        // Given
        given(claimService.createClaim(any())).willReturn(aClaimResult(ClaimStatus.NEW, Optional.empty()));
        Claimant claimant = aValidClaimant();
        given(claimantConverter.convert(any())).willReturn(claimant);
        Map mockStatusMap = mock(Map.class);
        ReflectionTestUtils.setField(controller, "statusMap", mockStatusMap);
        given(mockStatusMap.get(ClaimStatus.NEW)).willReturn(null);
        ClaimDTO dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResultDTO> response = controller.createClaim(dto);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(aClaimResultDTOWithClaimStatusAndNoVoucherEntitlement(ClaimStatus.NEW));
        verifyCreateOrUpdateClaimCalledCorrectly(claimant, dto);
        verify(mockStatusMap).get(ClaimStatus.NEW);
        verify(claimantConverter).convert(dto.getClaimant());
    }

    private void verifyCreateOrUpdateClaimCalledCorrectly(Claimant claimant, ClaimDTO dto) {
        ArgumentCaptor<ClaimRequest> captor = ArgumentCaptor.forClass(ClaimRequest.class);
        verify(claimService).createClaim(captor.capture());
        ClaimRequest claimRequest = captor.getValue();
        assertThat(claimRequest).isNotNull();
        assertThat(claimRequest.getClaimant()).isEqualTo(claimant);
        assertThat(claimRequest.getDeviceFingerprint()).isEqualTo(dto.getDeviceFingerprint());
        assertThat(claimRequest.getWebUIVersion()).isEqualTo(dto.getWebUIVersion());
    }

}
