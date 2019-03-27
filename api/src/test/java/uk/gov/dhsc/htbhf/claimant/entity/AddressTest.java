package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.anAddressWithAddressLine1;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.anAddressWithAddressLine2;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.anAddressWithPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.anAddressWithTownOrCity;

public class AddressTest extends AbstractValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"EC11BB", "W1A0AX", "M11AE", "B338TH", "CR26XH", "DN551PT", "DN55 1PT"})
    void shouldValidateAddressSuccessfully(String postcode) {
        //Given
        var address = anAddressWithPostcode(postcode);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldValidateAddressSuccessfullyWithNoAddressLine2() {
        //Given
        var address = anAddressWithAddressLine2(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateAddressWithNoAddressLine1() {
        //Given
        var address = anAddressWithAddressLine1(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWhenAddressLine1IsTooLong() {
        //Given
        var address = anAddressWithAddressLine1(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWhenAddressLine2IsTooLong() {
        //Given
        var address = anAddressWithAddressLine2(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "addressLine2");
    }

    @Test
    void shouldFailToValidateAddressWithNoTownOrCity() {
        //Given
        var address = anAddressWithTownOrCity(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWhenTownOrCityIsTooLong() {
        //Given
        var address = anAddressWithTownOrCity(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWithNoPostcode() {
        //Given
        var address = anAddressWithPostcode(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA1122BB", "A", "11AA21", "", "E!", "EA123"})
    void shouldFailToValidateAddressWithInvalidPostcode(String postcode) {
        //Given
        var address = anAddressWithPostcode(postcode);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid postcode format", "postcode");
    }
}
