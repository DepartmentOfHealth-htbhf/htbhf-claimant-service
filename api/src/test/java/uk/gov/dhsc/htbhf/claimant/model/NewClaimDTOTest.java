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

import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithPhoneNumber;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityOverrideDTOTestDataFactory.aConfirmedEligibilityOverrideDTOWithChildren;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CHILD_BORN_IN_FUTURE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

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
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(EligibilityOverrideDTO.builder().build());
        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasTotalViolations(3);
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.eligibilityOutcome");
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.overrideUntil");
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.childrenDob");
    }

    @Test
    void shouldFailToValidateClaimWithInvalidEligibilityOverrideWithChildrenBornInFuture() {
        //Given
        EligibilityOverrideDTO eligibilityOverrideDTO = aConfirmedEligibilityOverrideDTOWithChildren(CHILD_BORN_IN_FUTURE);
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(eligibilityOverrideDTO);

        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasTotalViolations(1);
        assertThat(violations).hasViolation("dates of birth of children should be all in the past", "eligibilityOverride.childrenDob");
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
        EligibilityOverrideDTO eligibilityOverrideDTO = aConfirmedEligibilityOverrideDTOWithChildren(MAGGIE_AND_LISA_DOBS);
        NewClaimDTO claim = aValidClaimDTOWithEligibilityOverride(eligibilityOverrideDTO);

        //When
        Set<ConstraintViolation<NewClaimDTO>> violations = validator.validate(claim);

        //Then
        assertThat(violations).hasNoViolations();
    }
}
