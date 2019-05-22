package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.*;

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

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        Message message = Message.builder().build();
        //When
        UUID id = message.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        Message message = Message.builder().build();
        ReflectionTestUtils.setField(message, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(message.getId());
    }
}
