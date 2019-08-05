package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.*;

public class AddressTest extends AbstractValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {"EC11BB", "W1A0AX", "M11AE", "B338TH", "CR26XH", "DN551PT", "DN55 1PT"})
    void shouldValidateAddressSuccessfully(String postcode) {
        //Given
        Address address = anAddressWithPostcode(postcode);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldValidateAddressSuccessfullyWithNoAddressLine2() {
        //Given
        Address address = anAddressWithAddressLine2(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldValidateAddressSuccessfullyWithNoCounty() {
        //Given
        Address address = anAddressWithCounty(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateAddressWithNoAddressLine1() {
        //Given
        Address address = anAddressWithAddressLine1(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWhenAddressLine1IsTooLong() {
        //Given
        Address address = anAddressWithAddressLine1(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "addressLine1");
    }

    @Test
    void shouldFailToValidateAddressWhenAddressLine2IsTooLong() {
        //Given
        Address address = anAddressWithAddressLine2(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "addressLine2");
    }

    @Test
    void shouldFailToValidateAddressWithNoTownOrCity() {
        //Given
        Address address = anAddressWithTownOrCity(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWhenTownOrCityIsTooLong() {
        //Given
        Address address = anAddressWithTownOrCity(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "townOrCity");
    }

    @Test
    void shouldFailToValidateAddressWhenCountyIsTooLong() {
        //Given
        Address address = anAddressWithCounty(LONG_STRING);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "county");
    }

    @Test
    void shouldFailToValidateAddressWithNoPostcode() {
        //Given
        Address address = anAddressWithPostcode(null);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "postcode");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA1122BB", "A", "11AA21", "", "E!", "EA123"})
    void shouldFailToValidateAddressWithInvalidPostcode(String postcode) {
        //Given
        Address address = anAddressWithPostcode(postcode);
        //When
        Set<ConstraintViolation<Address>> violations = validator.validate(address);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid postcode format", "postcode");
    }

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        Address address = Address.builder().build();
        //When
        UUID id = address.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        Address address = Address.builder().build();
        ReflectionTestUtils.setField(address, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(address.getId());
    }
}
