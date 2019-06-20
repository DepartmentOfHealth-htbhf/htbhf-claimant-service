package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.*;

class ClaimantTest extends AbstractValidationTest {

    @Test
    void shouldValidateClaimantSuccessfully() {
        //Given
        Claimant claimant = aValidClaimant();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimantWithNoLastName() {
        //Given
        Claimant claimant = aClaimantWithLastName(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithTooLongFirstName() {
        //Given
        Claimant claimant = aClaimantWithTooLongFirstName();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 500", "firstName");
    }

    @Test
    void shouldFailToValidateClaimantWithTooLongSurname() {
        //Given
        Claimant claimant = aClaimantWithTooLongLastName();
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithBlankSurname() {
        //Given
        Claimant claimant = aClaimantWithLastName("");
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 1 and 500", "lastName");
    }

    @Test
    void shouldFailToValidateClaimantWithInvalidFirstNameAndSurname() {
        //Given
        Claimant claimant = aClaimantWithFirstNameAndLastName(LONG_NAME, "");
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

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        Claimant claimant = Claimant.builder().build();
        ReflectionTestUtils.setField(claimant, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(claimant.getId());
    }

    @Test
    void shouldFailToValidateClaimantWithNoNino() {
        //Given
        String nino = null;
        Claimant claimant = aClaimantWithNino(nino);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "nino");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "YYHU456781", "Y*U", "888888888", "ABCDEFGHI", "ZQQ123456CZ", "QQ123456T", "QQ 12 34 56 D"})
    void shouldFailToValidateClaimantWithInvalidFormatNino(String invalidNino) {
        //Given
        Claimant claimant = aClaimantWithNino(invalidNino);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must match \"[a-zA-Z]{2}\\d{6}[a-dA-D]\"", "nino");
    }

    @Test
    void shouldFailToValidateClaimantWithNoDateOfBirth() {
        //Given
        LocalDate dateOfBirth = null;
        Claimant claimant = aClaimantWithDateOfBirth(dateOfBirth);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "dateOfBirth");
    }

    @Test
    void shouldFailToValidateClaimantWithDateOfBirthInFuture() {
        //Given
        LocalDate dateOfBirth = LocalDate.parse("9999-12-30");
        Claimant claimant = aClaimantWithDateOfBirth(dateOfBirth);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must be a past date", "dateOfBirth");
    }

    @Test
    void shouldValidateClaimantWithoutExpectedDueDate() {
        //Given
        Claimant claimant = aClaimantWithExpectedDeliveryDate(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldValidateClaimantWithExpectedDueDate() {
        //Given
        Claimant claimant = aClaimantWithExpectedDeliveryDate(LocalDate.now());
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldValidateClaimantWithExpectedDueDateInThePast() {
        //Given
        Claimant claimant = aClaimantWithExpectedDeliveryDate(LocalDate.now().minusYears(3));
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidateClaimantWithoutCardDeliveryAddress() {
        //Given
        Claimant claimant = aClaimantWithCardDeliveryAddress(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "address");
    }

    @Test
    void shouldFailToValidateClaimantWithoutPhoneNumber() {
        //Given
        Claimant claimant = aClaimantWithPhoneNumber(null);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+4471234567891",
            "+44712345",
            "07123456789",
            "00447123456789"
    })
    void shouldFailToValidateClaimantWithPhoneNumberInWrongFormat(String phoneNumber) {
        //Given
        Claimant claimant = aClaimantWithPhoneNumber(phoneNumber);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid UK phone number, must be in +447123456789 format", "phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+447123456789",
            "+44212345678", // non mobile numbers can be 9 digits after +44
    })
    void shouldValidateClaimantWithPhoneNumberInCorrectFormat(String phoneNumber) {
        //Given
        Claimant claimant = aClaimantWithPhoneNumber(phoneNumber);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "#@%^%#$@#$@#.com",
            "@domain.com",
            "Joe Smith <email@domain.com>",
            "email.domain.com",
            "email@domain.com (Joe Smith)",
            "@email.com",
            "test@domain"
    })
    void shouldFailToValidateInvalidEmailAddress(String emailAddress) {
        //Given
        Claimant claimant = aClaimantWithEmailAddress(emailAddress);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("invalid email address", "emailAddress");
    }

    @Test
    void shouldFailToValidateEmailAddressTooLong() {
        //Given
        String longEmailAddress = CharBuffer.allocate(256).toString().replace('\0', 'A') + "@email.com";
        Claimant claimant = aClaimantWithEmailAddress(longEmailAddress);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasSingleConstraintViolation("size must be between 0 and 256", "emailAddress");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "email@domain.com",
            "firstname.lastname@domain.com",
            "email@subdomain.domain.com",
            "firstname+lastname@domain.com",
            "1234567890@domain.com",
            "Email@domain-one.com",
            "_______@domain.com",
            "email@domain.name",
            "email@domain.co.jp",
            "firstname-lastname@domain.com",
            "Pälé@example.com"
    })
    void shouldValidateClaimantWithValidEmailAddress(String emailAddress) {
        //Given
        Claimant claimant = aClaimantWithEmailAddress(emailAddress);
        //When
        Set<ConstraintViolation<Claimant>> violations = validator.validate(claimant);
        //Then
        assertThat(violations).hasNoViolations();
    }
}
