package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aClaimantDTOWithPhoneNumber;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aClaimDTOWithClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aClaimDTOWithDeviceFingerprint;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aClaimDTOWithWebUIVersion;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.aValidClaimDTO;

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

}
