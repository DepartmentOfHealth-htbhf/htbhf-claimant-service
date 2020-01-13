package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class ClaimToClaimDTOConverterTest {

    @Mock
    private AddressToAddressDTOConverter addressToAddressDTOConverter;
    @InjectMocks
    private ClaimToClaimDTOConverter converter;

    @Test
    void shouldConvertClaimToClaimDTO() {
        Claim claim = aValidClaim();
        AddressDTO address = aValidAddressDTO();
        given(addressToAddressDTOConverter.convert(any())).willReturn(address);

        ClaimDTO claimDTO = converter.convert(claim);

        assertThat(claimDTO).isEqualToComparingOnlyGivenFields(claim,
                "id", "cardAccountId", "cardStatus", "cardStatusTimestamp", "claimStatus", "claimStatusTimestamp", "currentIdentityAndEligibilityResponse",
                "dwpHouseholdIdentifier", "hmrcHouseholdIdentifier",  "eligibilityStatus", "eligibilityStatusTimestamp",
                "initialIdentityAndEligibilityResponse");
        assertThat(claimDTO.getClaimant()).isEqualToIgnoringGivenFields(claim.getClaimant(), "address");
        assertThat(claimDTO.getClaimant().getAddress()).isEqualTo(address);
        verify(addressToAddressDTOConverter).convert(claim.getClaimant().getAddress());
    }
}
