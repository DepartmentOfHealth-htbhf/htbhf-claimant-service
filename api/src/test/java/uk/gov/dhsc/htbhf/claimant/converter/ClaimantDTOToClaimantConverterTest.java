package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aValidClaimantDTOWithNoNullFields;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.MAGGIE_DATE_OF_BIRTH;

@ExtendWith(MockitoExtension.class)
class ClaimantDTOToClaimantConverterTest {

    private static final Address ADDRESS = aValidAddress();

    @Mock
    private AddressDTOToAddressConverter addressConverter;
    @InjectMocks
    private ClaimantDTOToClaimantConverter converter;

    @Test
    void shouldConvertClaimDTOToEquivalentClaimObject() {
        // Given
        ClaimantDTO claimantDTO = aValidClaimantDTOWithNoNullFields();
        when(addressConverter.convert(claimantDTO.getAddress())).thenReturn(ADDRESS);

        // When
        Claimant result = converter.convert(claimantDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(claimantDTO.getFirstName());
        assertThat(result.getLastName()).isEqualTo(claimantDTO.getLastName());
        assertThat(result.getNino()).isEqualTo(claimantDTO.getNino());
        assertThat(result.getDateOfBirth()).isEqualTo(claimantDTO.getDateOfBirth());
        assertThat(result.getExpectedDeliveryDate()).isEqualTo(claimantDTO.getExpectedDeliveryDate());
        assertThat(result.getAddress()).isEqualTo(ADDRESS);
        assertThat(result.getPhoneNumber()).isEqualTo(claimantDTO.getPhoneNumber());
        assertThat(result.getEmailAddress()).isEqualTo(claimantDTO.getEmailAddress());
        assertThat(result.getChildrenDob()).containsExactly(MAGGIE_DATE_OF_BIRTH);
        verify(addressConverter).convert(claimantDTO.getAddress());
    }

    @Test
    void shouldEnsureNinoIsUppercased() {
        // Given
        String nino = "AA123456A";
        ClaimantDTO claimantDTO = aClaimantDTOWithNino(nino.toLowerCase());
        when(addressConverter.convert(claimantDTO.getAddress())).thenReturn(ADDRESS);

        // When
        Claimant result = converter.convert(claimantDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNino()).isEqualTo(nino);
    }

    @Test
    void shouldNotConvertNullClaimDTO() {
        assertThatIllegalArgumentException().isThrownBy(() -> converter.convert(null));
    }

}
