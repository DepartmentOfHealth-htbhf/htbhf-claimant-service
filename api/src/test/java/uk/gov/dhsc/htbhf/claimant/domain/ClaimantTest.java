package uk.gov.dhsc.htbhf.claimant.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantTest {

    Validator validator;

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
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldValidateClaimantWithNoSurname() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithSecondName(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertViolationPresent(violations, "must not be null", "secondName");
    }

    @Test
    void shouldValidateClaimantWithTooLongSurname() {
        //Given
        Claimant claimant = ClaimantTestDataFactory.aClaimantWithTooLongSecondName();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).isNotEmpty();
        assertViolationPresent(violations, "length must be between 0 and 500", "secondName");
    }

    //TODO - Change to use custom AssertJ assertion (http://joel-costigliola.github.io/assertj/assertj-core-custom-assertions.html)
    private void assertViolationPresent(Set<ConstraintViolation<Claimant>> violations, String message, String path) {
        assertThat(violations).isNotEmpty();
        assertThat(violations).hasSize(1);
        ConstraintViolation<Claimant> violation = violations.iterator().next();
        assertThat(violation.getMessage()).isEqualTo(message);
        assertThat(violation.getPropertyPath().toString()).isEqualTo(path);
    }
}