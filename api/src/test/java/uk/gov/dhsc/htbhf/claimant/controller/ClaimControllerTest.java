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
import uk.gov.dhsc.htbhf.claimant.converter.ClaimantDTOToClaimantConverter;
import uk.gov.dhsc.htbhf.claimant.converter.VoucherEntitlementToDTOConverter;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.VoucherEntitlementDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimResult;
import uk.gov.dhsc.htbhf.claimant.service.NewClaimService;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResponseTestDataFactory.aClaimResponseWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimResponseTestDataFactory.aClaimResponseWithClaimStatusAndNoVoucherEntitlement;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatus;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementDTOTestDataFactory.aValidVoucherEntitlementDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    NewClaimService newClaimService;

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
        Claimant claimant = aValidClaimantBuilder().build();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.of(aValidVoucherEntitlement()));
        VoucherEntitlementDTO entitlementDTO = aValidVoucherEntitlementDTO();
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(entitlementConverter.convert(any())).willReturn(entitlementDTO);
        given(newClaimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithClaimStatus(claimStatus);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(newClaimService).createClaim(claimant);
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
        Claimant claimant = aValidClaimantBuilder().build();
        ClaimResult claimResult = aClaimResult(claimStatus, Optional.empty());
        given(claimantConverter.convert(any())).willReturn(claimant);
        given(newClaimService.createClaim(any())).willReturn(claimResult);

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithClaimStatusAndNoVoucherEntitlement(claimStatus);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(newClaimService).createClaim(claimant);
        verify(claimantConverter).convert(dto.getClaimant());
        verifyZeroInteractions(entitlementConverter);
    }

    @Test
    void shouldReturnInternalServerErrorStatusWhenEligibilityStatusIsInvalid() {
        // Given
        given(newClaimService.createClaim(any())).willReturn(aClaimResult(ClaimStatus.NEW, Optional.empty()));
        Claimant claimant = aValidClaimantBuilder().build();
        given(claimantConverter.convert(any())).willReturn(claimant);
        Map mockStatusMap = mock(Map.class);
        ReflectionTestUtils.setField(controller, "statusMap", mockStatusMap);
        given(mockStatusMap.get(ClaimStatus.NEW)).willReturn(null);
        ClaimDTO dto = aValidClaimDTO();

        // When
        ResponseEntity<ClaimResponse> response = controller.newClaim(dto);

        // Then
        ClaimResponse claimResponse = aClaimResponseWithClaimStatusAndNoVoucherEntitlement(ClaimStatus.NEW);
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(claimResponse);
        verify(newClaimService).createClaim(claimant);
        verify(mockStatusMap).get(ClaimStatus.NEW);
        verify(claimantConverter).convert(dto.getClaimant());
    }

    private ClaimResult aClaimResult(ClaimStatus claimStatus, Optional<VoucherEntitlement> voucherEntitlement) {
        return ClaimResult.builder()
                .claim(aClaimWithClaimStatus(claimStatus))
                .voucherEntitlement(voucherEntitlement)
                .build();
    }
}
