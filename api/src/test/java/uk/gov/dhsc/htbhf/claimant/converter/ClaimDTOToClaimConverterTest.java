package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTOWithNoNullFields;

@ExtendWith(MockitoExtension.class)
class ClaimDTOToClaimConverterTest {

    private static final Address ADDRESS = aValidAddress();

    @Mock
    private AddressDTOToAddressConverter addressConverter;
    @InjectMocks
    private ClaimDTOToClaimConverter converter;

    @Test
    void shouldConvertClaimDTOToEquivalentClaimObject() {
        // Given
        ClaimDTO claimDTO = aValidClaimDTOWithNoNullFields();
        ClaimantDTO claimantDTO = claimDTO.getClaimant();
        when(addressConverter.convert(claimantDTO.getCardDeliveryAddress())).thenReturn(ADDRESS);

        // When
        Claim result = converter.convert(claimDTO);

        // Then
        assertThat(result).isNotNull();
        Claimant claimant = result.getClaimant();
        assertThat(claimant).isNotNull();
        assertThat(claimant.getFirstName()).isEqualTo(claimantDTO.getFirstName());
        assertThat(claimant.getLastName()).isEqualTo(claimantDTO.getLastName());
        assertThat(claimant.getNino()).isEqualTo(claimantDTO.getNino());
        assertThat(claimant.getDateOfBirth()).isEqualTo(claimantDTO.getDateOfBirth());
        assertThat(claimant.getExpectedDeliveryDate()).isEqualTo(claimantDTO.getExpectedDeliveryDate());
        assertThat(claimant.getCardDeliveryAddress()).isEqualTo(ADDRESS);
        verify(addressConverter).convert(claimDTO.getClaimant().getCardDeliveryAddress());
    }

    @Test
    void shouldNotConvertNullClaimDTO() {
        assertThatIllegalArgumentException().isThrownBy(() -> converter.convert(null));
    }

}
