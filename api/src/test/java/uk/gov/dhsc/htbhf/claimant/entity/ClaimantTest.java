package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static uk.gov.dhsc.htbhf.claimant.assertion.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.LONG_NAME;

class ClaimantTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void shouldValidateClaimantSuccessfully() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aValidClaimant();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimantWithNoLastName() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithLastName(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithTooLongFirstName() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithTooLongFirstName();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "firstName");
    }

    @Test
    void shouldFailToValidateClaimantWithTooLongSurname() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithTooLongLastName();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithBlankSurname() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithLastName("");
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithInvalidFirstNameAndSurname() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithFirstNameAndLastName(LONG_NAME, "");
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasTotalViolations(2)
                .hasViolation("size must be between 1 and 500", "lastName")
                .hasViolation("size must be between 0 and 500", "firstName");
    }

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        Claimant claimant = Claimant.builder().build();
        //When
        UUID id = claimant.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

}
