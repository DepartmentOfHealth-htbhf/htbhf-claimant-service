package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimResponseDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.TestConstants.MARGE_NINO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimWithNinoAndRefernce;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HOMER_CLAIM_REFERENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.MARGE_CLAIM_REFERENCE;


@ExtendWith(MockitoExtension.class)
class ClaimToClaimResponseDTOConverterTest {

    @InjectMocks
    private ClaimToClaimResponseDTOConverter converter;

    @Test
    void shouldConvertClaimToClaimDTO() {
        Claim homerClaim = aValidClaimWithNinoAndRefernce(HOMER_NINO, HOMER_CLAIM_REFERENCE);
        Claim margeClaim = aValidClaimWithNinoAndRefernce(MARGE_NINO, MARGE_CLAIM_REFERENCE);

        List<ClaimResponseDTO> claimResponse = converter.convert(List.of(homerClaim, margeClaim));

        assertThat(claimResponse).hasSameSizeAs(List.of(homerClaim, margeClaim));
        assertThat(claimResponse.get(0).getFirstName()).isEqualTo(homerClaim.getClaimant().getFirstName());
        assertThat(claimResponse.get(0).getLastName()).isEqualTo(homerClaim.getClaimant().getLastName());
        assertThat(claimResponse.get(0).getReference()).isEqualTo(homerClaim.getReference());
        assertThat(claimResponse.get(0).getDateOfBirth()).isEqualTo(homerClaim.getClaimant().getDateOfBirth());


    }

}
