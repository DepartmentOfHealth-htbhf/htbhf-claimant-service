package uk.gov.dhsc.htbhf.claimant.model.v3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOV3TestDataFactory.*;

class AddressDTOV3Test extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateAddress() {
        //Given
        AddressDTOV3 addressDTO = aValidAddressDTO();
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateAddressWithNoAddressLine2() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithLine2(null);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateAddressWithNoCounty() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithCounty(null);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateAddressWithEmptyStringCounty() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithCounty("");
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateAddressWithNoLine1() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithLine1(null);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongLine1() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithLine1(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongLine2() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithLine2(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "addressLine2");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongTownOrCity() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithTownOrCity(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWithTooLongCounty() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithCounty(LONG_STRING);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "county");
    }

    @Test
    void shouldFailToValidateAddressWithNullTownOrCity() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithTownOrCity(null);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWithNullPostcode() {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode(null);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "AA1122BB",
            "A",
            "11AA21",
            "DE551IM",
    })
    void shouldFailValidationWithInvalidPostcode(String postcode) {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid postcode format", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GY1 1WR",
            "JE3 1FU",
            "IM1 3LY"
    })
    void shouldFailValidationWithChannelIslandPostcode(String postcode) {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasSingleConstraintViolation("postcodes in the Channel Islands or Isle of Man are not acceptable", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Gy1 1wr",
            "je311fu",
            "im13ly"
    })
    void shouldFailValidationWithChannelIslandPostcodeAndInvalidFormat(String postcode) {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasTotalViolations(2)
                .hasViolation("postcodes in the Channel Islands or Isle of Man are not acceptable", "postcode")
                .hasViolation("invalid postcode format", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GA11BB",
            "JA10AX",
            "M11AE",
            "IO338GY",
            "CY26JE",
            "DM55 1PT"
    })
    void shouldPassValidationWithNonChannelIslandPostcode(String postcode) {
        //Given
        AddressDTOV3 addressDTO = anAddressDTOWithPostcode(postcode);
        //When
        Set<ConstraintViolation<AddressDTOV3>> violations = validator.validate(addressDTO);
        //Then
        assertThat(violations).hasNoViolations();
    }

}
