package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.*;

class AddressDTOTest extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateAddress() {
        //Given
        AddressDTO addressDTO = aValidAddressDTO();
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateAddressWithNoLine1() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine1(null);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongLine1() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine1(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongLine2() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine2(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "addressLine2");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongTownOrCity() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithTownOrCity(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWithNullTownOrCity() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithTownOrCity(null);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWithNullPostcode() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode(null);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "AA1122BB",
            "A",
            "11AA21",
    })
    void shouldFailValidationWithInvalidPostcode(String postcode) {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid postcode format", "postcode");
    }

}
