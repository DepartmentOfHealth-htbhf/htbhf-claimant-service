package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentTestDataFactory.*;

class PaymentTest extends AbstractValidationTest {
    @Test
    void shouldValidatePaymentSuccessfully() {
        //Given
        Payment payment = aValidPayment();
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidatePaymentWithNoClaim() {
        //Given
        Payment payment = aPaymentWithClaim(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claim");
    }

    @Test
    void shouldFailToValidatePaymentWithNoCardAccountId() {
        //Given
        Payment payment = aPaymentWithCardAccountId(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "cardAccountId");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentAmountInPence() {
        //Given
        Payment payment = aPaymentWithPaymentAmountInPence(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentAmountInPence");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentTimestamp() {
        //Given
        Payment payment = aPaymentWithPaymentTimestamp(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentTimestamp");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentStatus() {
        //Given
        Payment payment = aPaymentWithPaymentStatus(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentStatus");
    }

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        Payment payment = Payment.builder().build();
        //When
        UUID id = payment.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        Payment payment = Payment.builder().build();
        ReflectionTestUtils.setField(payment, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(payment.getId());
    }
}
