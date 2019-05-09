package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
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
}
