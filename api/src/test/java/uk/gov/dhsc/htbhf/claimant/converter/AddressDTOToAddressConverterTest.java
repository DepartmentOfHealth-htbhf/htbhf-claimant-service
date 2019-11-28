package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithPostcode;

class AddressDTOToAddressConverterTest {

    private AddressDTOToAddressConverter converter = new AddressDTOToAddressConverter();

    @Test
    void shouldConvertAddressDTOToEquivalentAddressObject() {
        // Given
        AddressDTO addressDTO = aValidAddressDTO();

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
        AddressDTO addressDTO = anAddressDTOWithPostcode("aa11aa");

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
