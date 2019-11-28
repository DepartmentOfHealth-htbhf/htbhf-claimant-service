package uk.gov.dhsc.htbhf.claimant.model.v2;

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
    void shouldSuccessfullyValidateAddressWithNoAddressLine2() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine2(null);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateAddressWithNoCounty() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithCounty(null);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateAddressWithEmptyStringCounty() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithCounty("");
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
    void shouldFailToValidateAddressWithTooLongCounty() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithCounty(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "county");
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

    @ParameterizedTest
    @ValueSource(strings = {
            "GY1 1WR",
            "JE3 1FU",
            "IM1 3LY",
            "Gy1 1wr",
            "je311fu",
            "im13ly",
    })
    void shouldFailValidationWithChannelIslandPostcode(String postcode) {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("postcodes in the Channel Islands or Isle of Man are not acceptable", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GA11BB",
            "JA10AX",
            "M11AE",
            "IO338GY",
            "CY26JE",
            "DE551IM",
            "DM55 1PT",
    })
    void shouldPassValidationWithNonChannelIslandPostcode(String postcode) {
        //Given
        AddressDTO addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTO>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

}
