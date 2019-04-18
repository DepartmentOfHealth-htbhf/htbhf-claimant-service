package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithPayload;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;

class MessageTest extends AbstractValidationTest {

    @Test
    void shouldSuccessfullyValidateAValidMessage() {
        //Given
        Message message = aValidMessage();
        //When
        Set<ConstraintViolation<Message>> violations = validator.validate(message);
        //Then
        assertThat(violations).hasNoViolations();

    }

    @Test
    void shouldFailToValidateWithNoMessageType() {
        //Given
        Message message = aValidMessageWithType(null);
        //When
        Set<ConstraintViolation<Message>> violations = validator.validate(message);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "messageType");
    }

    @Test
    void shouldFailToValidateWithNoTimestamp() {
        //Given
        Message message = aValidMessageWithTimestamp(null);
        //When
        Set<ConstraintViolation<Message>> violations = validator.validate(message);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "messageTimestamp");
    }

    @Test
    void shouldFailToValidateWithNoPayload() {
        //Given
        Message message = aValidMessageWithPayload(null);
        //When
        Set<ConstraintViolation<Message>> violations = validator.validate(message);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "messagePayload");
    }
}
