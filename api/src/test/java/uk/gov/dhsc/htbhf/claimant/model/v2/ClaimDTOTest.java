package uk.gov.dhsc.htbhf.claimant.model.v2;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aClaimDTOWithDeviceFingerprint;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aClaimDTOWithWebUIVersion;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.aValidClaimDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithPhoneNumber;

class ClaimDTOTest extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateClaim() {
        //Given
        ClaimDTO claim = aValidClaimDTO();
        //When
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoDeviceFingerprint() {
        //Given
        ClaimDTO claim = aClaimDTOWithDeviceFingerprint(null);
        //When
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldSuccessfullyValidateClaimWithNoWebUIVersion() {
        //Given
        ClaimDTO claim = aClaimDTOWithWebUIVersion(null);
        //When
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimWithInvalidClaimant() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithPhoneNumber(null);
        ClaimDTO claim = aClaimDTOWithClaimant(claimant);
        //When
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant.phoneNumber");
    }

    @Test
    void shouldFailToValidateClaimWithNoClaimant() {
        //Given
        ClaimDTO claim = aClaimDTOWithClaimant(null);
        //When
        Set<ConstraintViolation<ClaimDTO>> violations = validator.validate(claim);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimant");
    }

}
