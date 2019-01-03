package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ClaimDTOToClaimConverterTest {

    private ClaimDTOToClaimConverter converter = new ClaimDTOToClaimConverter();

    @Test
    void shouldConvertClaimDTOToEquivalentClaimObject() {
        // Given
        ClaimDTO claimDTO = ClaimDTOTestDataFactory.aValidClaimDTO();
        ClaimantDTO claimantDTO = claimDTO.getClaimant();

        // When
        Claim result = converter.convert(claimDTO);

        // Then
        assertThat(result).isNotNull();
        Claimant claimant = result.getClaimant();
        assertThat(claimant).isNotNull();
        assertThat(claimant.getFirstName()).isEqualTo(claimantDTO.getFirstName());
        assertThat(claimant.getSecondName()).isEqualTo(claimantDTO.getSecondName());
    }

    @Test
    void shouldNotConvertNullClaimDTO() {
        assertThatIllegalArgumentException().isThrownBy(() -> converter.convert(null));
    }

}
