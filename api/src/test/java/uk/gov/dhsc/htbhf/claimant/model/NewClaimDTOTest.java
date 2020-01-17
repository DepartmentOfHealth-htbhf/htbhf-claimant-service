package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithPhoneNumber;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;

class NewClaimDTOTest extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateClaim() {
        //Given
        NewClaimDTO claim = aValidClaimDTO();
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoDeviceFingerprint() {
        //Given
        NewClaimDTO claim = aClaimDTOWithDeviceFingerprint(null);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoWebUIVersion() {
        //Given
        NewClaimDTO claim = aClaimDTOWithWebUIVersion(null);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimWithInvalidClaimant() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithPhoneNumber(null);
        NewClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant.phoneNumber");
    }

    @Test
    void shouldFailToValidateClaimWithNoClaimant() {
        //Given
        NewClaimDTO claim = aClaimDTOWithClaimant(null);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant");
    }

    @Test
    void shouldFailToValidateClaimWithInvalidEligibilityOverride() {
        //Given
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(null, null, null, null);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasTotalViolations(2);
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.eligibilityOutcome");
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.overrideUntil");

    }

    @ParameterizedTest
    @MethodSource("datesNotInFuture")
    void shouldFailToValidateClaimWithEligibilityOverrideUntilDatesNotInFuture(LocalDate untilDate) {
        //Given
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                NO_CHILDREN,
                EligibilityOutcome.CONFIRMED,
                untilDate);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasTotalViolations(1);
        assertThat(violations).hasViolation("must be a future date", "eligibilityOverride.overrideUntil");

    }

    private static Stream<LocalDate> datesNotInFuture() {
        return Stream.of(
                LocalDate.now(),
                LocalDate.now().minusDays(1)
        );
    }

    @Test
    void shouldSuccessfullyValidateClaimWithEligibilityOverride() {
        //Given
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(
                EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS,
                NO_CHILDREN,
                EligibilityOutcome.CONFIRMED,
                OVERRIDE_UNTIL_FIVE_YEARS);
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }
}
