package uk.gov.dhsc.htbhf.claimant.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithLine1;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.*;

class ClaimantDTOTest extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateClaimant() {
        //Given
        ClaimantDTO claimant = aValidClaimantDTO();
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailValidationWithNullFirstName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithFirstName(null);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "firstName");
    }

    @Test
    void shouldFailValidationWithTooLongFirstName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithFirstName(LONG_STRING);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "firstName");
    }

    @Test
    void shouldFailValidationWithBlankFirstName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithFirstName("");
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "firstName");
    }

    @Test
    void shouldFailValidationWithNullLastName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithLastName(null);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "lastName");
    }

    @Test
    void shouldFailValidationWithTooLongLastName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithLastName(LONG_STRING);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailValidationWithBlankLastName() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithLastName("");
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailValidationWithNullNino() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithNino(null);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "nino");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "YYHU456781",
            "888888888",
            "ABCDEFGHI",
            "ZQQ123456CZ",
            "QQ123456C",
            "ZZ999999D"
    })
    void shouldFailValidationWithInvalidNino(String nino) {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithNino(nino);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must match \"^(?!BG|GB|NK|KN|TN|NT|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z](\\d{6})[A-D]$\"",
                "nino");
    }

    @Test
    void shouldFailValidationWithNullDateOfBirth() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithDateOfBirth(null);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "dateOfBirth");
    }

    @Test
    void shouldFailValidationWithDateOfBirthInFuture() {
        //Given
        LocalDate dateInFuture = LocalDate.now().plusYears(1);
        ClaimantDTO claimant = aClaimantDTOWithDateOfBirth(dateInFuture);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must be a past date", "dateOfBirth");
    }

    @Test
    void shouldFailValidationWithExpectedDeliveryDateTooFarInFuture() {
        //Given
        LocalDate dateInFuture = LocalDate.now().plusYears(2);
        ClaimantDTO claimant = aClaimantDTOWithExpectedDeliveryDate(dateInFuture);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be more than one month in the past or 8 months in the future",
                "expectedDeliveryDate");
    }

    @Test
    void shouldFailValidationWithExpectedDeliveryDateTooFarInPast() {
        //Given
        LocalDate dateInPast = LocalDate.now().minusYears(2);
        ClaimantDTO claimant = aClaimantDTOWithExpectedDeliveryDate(dateInPast);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be more than one month in the past or 8 months in the future",
                "expectedDeliveryDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+4477009005",
            "+447700900516987679",
            "abcdef",
            "07123456789" // not in +44 format
    })
    void shouldFailWithInvalidPhoneNumber(String phoneNumber) {
        // Given
        ClaimantDTO claimant = aClaimantDTOWithPhoneNumber(phoneNumber);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid UK phone number, must be in +44 format, e.g. +447123456789", "phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "#@%^%#$@#$@#.com",
            "@domain.com"
    })
    void shouldFailWithInvalidEmailAddress(String emailAddress) {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithEmailAddress(emailAddress);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid email address", "emailAddress");
    }

    @Test
    void shouldFailWithTooLongEmailAddress() {
        //Given
        String longEmailAddress = CharBuffer.allocate(256).toString().replace('\0', 'A') + "@email.com";
        ClaimantDTO claimant = aClaimantDTOWithEmailAddress(longEmailAddress);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 256", "emailAddress");
    }

    @Test
    void shouldFailToValidateClaimantWithInvalidAddress() {
        //Given
        AddressDTO addressDTO = anAddressDTOWithLine1(null);
        ClaimantDTO claimant = aClaimantDTOWithAddress(addressDTO);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "address.addressLine1");
    }

    @Test
    void shouldFailToValidateClaimantWithNullAddress() {
        //Given
        ClaimantDTO claimant = aClaimantDTOWithAddress(null);
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "address");
    }

    @Test
    void shouldFailToValidateClaimantWithChildDobInFuture() {
        //Given
        LocalDate dateInFuture = LocalDate.now().plusYears(1);
        ClaimantDTO claimant = aClaimantDTOWithChildrenDob(Collections.singletonList(dateInFuture));
        //When
        Set<ConstraintViolation<ClaimantDTO>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("dates of birth of children should be all in the past", "initiallyDeclaredChildrenDob");
    }

}
