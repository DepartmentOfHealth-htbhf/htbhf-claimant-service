package uk.gov.dhsc.htbhf.claimant.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.converter.ClaimDTOToClaimConverter;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.service.ClaimService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    ClaimDTOToClaimConverter converter;
    @Mock
    ClaimService claimService;

    @InjectMocks
    ClaimController controller;

    @Test
    void shouldInvokeClaimServiceWithConvertedClaim() {
        // Given
        ClaimDTO dto = aValidClaimDTO();
        Claim convertedClaim = Claim.builder().build();
        given(converter.convert(dto)).willReturn(convertedClaim);

        // When
        controller.newClaim(dto);

        // Then
        verify(claimService).createClaim(convertedClaim);
    }

}