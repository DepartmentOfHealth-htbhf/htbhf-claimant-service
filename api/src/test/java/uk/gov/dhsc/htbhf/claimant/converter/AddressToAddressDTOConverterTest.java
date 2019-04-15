package uk.gov.dhsc.htbhf.claimant.converter;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;

class AddressToAddressDTOConverterTest {

    private AddressToAddressDTOConverter converter = new AddressToAddressDTOConverter();

    @Test
    void shouldConvertAddressDTOToEquivalentAddressObject() {
        // Given
        var address = aValidAddress();

        // When
        AddressDTO result = converter.convert(address);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddressLine1()).isEqualTo(address.getAddressLine1());
        assertThat(result.getAddressLine2()).isEqualTo(address.getAddressLine2());
        assertThat(result.getTownOrCity()).isEqualTo(address.getTownOrCity());
        assertThat(result.getPostcode()).isEqualTo(address.getPostcode());
    }

    @Test
    void shouldNotConvertNullAddressDTO() {
        assertThatIllegalArgumentException().isThrownBy(() -> converter.convert(null));
    }

}
