package uk.gov.dhsc.htbhf.claimant.model.v3;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aClaimDTOWithDeviceFingerprint;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aClaimDTOWithWebUIVersion;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOV3TestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOV3TestDataFactory.aClaimantDTOWithPhoneNumber;

class ClaimDTOV3Test extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateClaim() {
        //Given
        ClaimDTOV3 claim = aValidClaimDTO();
        //When
        Set<ConstraintViolation<ClaimDTOV3>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoDeviceFingerprint() {
        //Given
        ClaimDTOV3 claim = aClaimDTOWithDeviceFingerprint(null);
        //When
        Set<ConstraintViolation<ClaimDTOV3>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoWebUIVersion() {
        //Given
        ClaimDTOV3 claim = aClaimDTOWithWebUIVersion(null);
        //When
        Set<ConstraintViolation<ClaimDTOV3>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimWithInvalidClaimant() {
        //Given
        ClaimantDTOV3 claimant = aClaimantDTOWithPhoneNumber(null);
        ClaimDTOV3 claim = aClaimDTOWithClaimant(claimant);
        //When
        Set<ConstraintViolation<ClaimDTOV3>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant.phoneNumber");
    }

    @Test
    void shouldFailToValidateClaimWithNoClaimant() {
        //Given
        ClaimDTOV3 claim = aClaimDTOWithClaimant(null);
        //When
        Set<ConstraintViolation<ClaimDTOV3>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant");
    }

}
