package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOV3TestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOV3TestDataFactory.anAddressDTOWithPostcode;

class AddressDTOToAddressConverterTest {

    private AddressDTOToAddressConverter converter = new AddressDTOToAddressConverter();

    @Test
    void shouldConvertAddressDTOToEquivalentAddressObject() {
        // Given
        AddressDTOV3 addressDTO = aValidAddressDTO();

        // When
        Address result = converter.convert(addressDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isEqualTo(addressDTO.getAddressLine1());
        assertThat(result.getAddressLine2()).isEqualTo(addressDTO.getAddressLine2());
        assertThat(result.getTownOrCity()).isEqualTo(addressDTO.getTownOrCity());
        assertThat(result.getCounty()).isEqualTo(addressDTO.getCounty());
        assertThat(result.getPostcode()).isEqualTo(addressDTO.getPostcode());
    }

    @Test
    void shouldConvertPostcodeToUppercase() {
        // Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode("aa11aa");

        // When
        Address result = converter.convert(addressDTO);

        // Then
        assertThat(result.getPostcode()).isEqualTo("AA11AA");
    }

    @Test
    void shouldNotConvertNullAddressDTO() {
        assertThatIllegalArgumentException().isThrownBy(() -> converter.convert(null));
    }

}
