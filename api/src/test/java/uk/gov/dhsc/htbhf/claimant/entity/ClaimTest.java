package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithLastName;

class ClaimTest extends AbstractValidationTest {

    private static final LocalDateTime ONE_HOUR_AGO = LocalDateTime.now().minusHours(1);

    @Test
    void shouldValidateClaimSuccessfully() {
        //Given
        Claim claim = aValidClaim();
        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimWithoutClaimStatus() {
        //Given
        Claim claim = aClaimWithClaimStatus(null);
        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimStatus");
    }

    @Test
    void shouldFailToValidateClaimWithoutEligibilityStatus() {
        //Given
        Claim claim = aClaimWithEligibilityStatus(null);
        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "eligibilityStatus");
    }

    @Test
    void shouldFailToValidateClaimWithoutClaimant() {
        //Given
        Claim claim = aClaimWithClaimant(null);
        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant");
    }

    @Test
    void shouldFailToValidateClaimWithInvalidEligibilityOverride() {
        EligibilityOverride eligibilityOverride = EligibilityOverride.builder()
                .eligibilityOutcome(null)
                .overrideUntil(null)
                .childrenDob(null)
                .build();
        //Given
        Claim claim = aClaimWithEligibilityOverride(eligibilityOverride);

        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);

        assertThat(violations).hasTotalViolations(3);
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.eligibilityOutcome");
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.overrideUntil");
        assertThat(violations).hasViolation("must not be null", "eligibilityOverride.childrenDob");

    }

    @Test
    void shouldValidateClaimWithNullEligibilityOverride() {
        //Given
        Claim claim = aClaimWithEligibilityOverride(null);

        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);

        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimWithInvalidClaimant() {
        //Given
        Claimant invalidClaimant = aClaimantWithLastName(null);
        Claim claim = aClaimWithClaimant(invalidClaimant);
        //When
        Set<ConstraintViolation<Claim>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant.lastName");
    }

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        Claim claim = Claim.builder().build();
        //When
        UUID id = claim.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        Claim claim = Claim.builder().build();
        ReflectionTestUtils.setField(claim, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(claim.getId());
    }

    @Test
    void shouldUpdateClaimStatusAndTimestamp() {
        Claim claim = Claim.builder()
                .claimStatus(ClaimStatus.NEW)
                .claimStatusTimestamp(ONE_HOUR_AGO)
                .build();
        LocalDateTime now = LocalDateTime.now();

        claim.updateClaimStatus(ClaimStatus.ACTIVE);

        Assertions.assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        Assertions.assertThat(claim.getClaimStatusTimestamp()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldNotUpdateClaimStatusTimestampWhenStatusUnchanged() {
        Claim claim = Claim.builder()
                .claimStatus(ClaimStatus.ACTIVE)
                .claimStatusTimestamp(ONE_HOUR_AGO)
                .build();

        claim.updateClaimStatus(ClaimStatus.ACTIVE);

        Assertions.assertThat(claim.getClaimStatus()).isEqualTo(ClaimStatus.ACTIVE);
        Assertions.assertThat(claim.getClaimStatusTimestamp()).isEqualTo(ONE_HOUR_AGO);
    }

    @Test
    void shouldUpdateEligibilityStatusAndTimestamp() {
        Claim claim = Claim.builder()
                .eligibilityStatus(EligibilityStatus.INELIGIBLE)
                .eligibilityStatusTimestamp(ONE_HOUR_AGO)
                .build();
        LocalDateTime now = LocalDateTime.now();

        claim.updateEligibilityStatus(EligibilityStatus.ELIGIBLE);

        Assertions.assertThat(claim.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        Assertions.assertThat(claim.getEligibilityStatusTimestamp()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldNotUpdateEligibilityStatusTimestampWhenStatusUnchanged() {
        Claim claim = Claim.builder()
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .eligibilityStatusTimestamp(ONE_HOUR_AGO)
                .build();

        claim.updateEligibilityStatus(EligibilityStatus.ELIGIBLE);

        Assertions.assertThat(claim.getEligibilityStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        Assertions.assertThat(claim.getEligibilityStatusTimestamp()).isEqualTo(ONE_HOUR_AGO);
    }

    @Test
    void shouldUpdateCardStatusAndTimestamp() {
        Claim claim = Claim.builder()
                .cardStatus(CardStatus.ACTIVE)
                .cardStatusTimestamp(ONE_HOUR_AGO)
                .build();
        LocalDateTime now = LocalDateTime.now();

        claim.updateCardStatus(CardStatus.PENDING_CANCELLATION);

        Assertions.assertThat(claim.getCardStatus()).isEqualTo(CardStatus.PENDING_CANCELLATION);
        Assertions.assertThat(claim.getCardStatusTimestamp()).isAfterOrEqualTo(now);
    }

    @Test
    void shouldNotUpdateCardStatusTimestampWhenStatusUnchanged() {
        Claim claim = Claim.builder()
                .cardStatus(CardStatus.ACTIVE)
                .cardStatusTimestamp(ONE_HOUR_AGO)
                .build();

        claim.updateCardStatus(CardStatus.ACTIVE);

        Assertions.assertThat(claim.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        Assertions.assertThat(claim.getCardStatusTimestamp()).isEqualTo(ONE_HOUR_AGO);
    }
}
