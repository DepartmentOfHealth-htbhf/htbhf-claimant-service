package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideDTOTestDataFactory.aConfirmedEligibilityOverrideDTOWithChildren;

@ExtendWith(MockitoExtension.class)
class ClaimToClaimDTOConverterTest {

    @Mock
    private AddressToAddressDTOConverter addressToAddressDTOConverter;
    @Mock
    private EligibilityOverrideToEligibilityOverrideDTOConverter eligibilityOverrideDTOConverter;
    @InjectMocks
    private ClaimToClaimDTOConverter converter;

    @ParameterizedTest
    @MethodSource("eligibilityOverrideDTOValues")
    void shouldConvertClaimToClaimDTO(EligibilityOverrideDTO eligibilityOverrideDTO) {
        Claim claim = aValidClaim();
        AddressDTO address = aValidAddressDTO();
        given(addressToAddressDTOConverter.convert(any())).willReturn(address);
        given(eligibilityOverrideDTOConverter.convert(any())).willReturn(eligibilityOverrideDTO);

        ClaimDTO claimDTO = converter.convert(claim);

        assertThat(claimDTO).isEqualToComparingOnlyGivenFields(claim,
                "id", "cardAccountId", "cardStatus", "cardStatusTimestamp", "claimStatus", "claimStatusTimestamp", "currentIdentityAndEligibilityResponse",
                "dwpHouseholdIdentifier", "hmrcHouseholdIdentifier",  "eligibilityStatus", "eligibilityStatusTimestamp",
                "initialIdentityAndEligibilityResponse");
        assertThat(claimDTO.getClaimant()).isEqualToIgnoringGivenFields(claim.getClaimant(), "address");
        assertThat(claimDTO.getClaimant().getAddress()).isEqualTo(address);
        assertThat(claimDTO.getEligibilityOverride()).isEqualTo(eligibilityOverrideDTO);
        verify(addressToAddressDTOConverter).convert(claim.getClaimant().getAddress());
        verify(eligibilityOverrideDTOConverter).convert(claim.getEligibilityOverride());

    }

    private static Stream<EligibilityOverrideDTO> eligibilityOverrideDTOValues() {
        return Stream.of(
                null,
                aConfirmedEligibilityOverrideDTOWithChildren(MAGGIE_AND_LISA_DOBS)
        );
    }
}
