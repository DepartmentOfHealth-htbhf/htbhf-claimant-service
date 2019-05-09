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
        var payment = aValidPayment();
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidatePaymentWithNoClaim() {
        //Given
        var payment = aPaymentWithClaimId(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claimId");
    }

    @Test
    void shouldFailToValidatePaymentWithNoCardAccountId() {
        //Given
        var payment = aPaymentWithCardAccountId(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "cardAccountId");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentAmountInPence() {
        //Given
        var payment = aPaymentWithPaymentAmountInPence(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentAmountInPence");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentTimestamp() {
        //Given
        var payment = aPaymentWithPaymentTimestamp(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentTimestamp");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentReference() {
        //Given
        var payment = aPaymentWithPaymentReference(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentReference");
    }

    @Test
    void shouldFailToValidatePaymentWithNoPaymentStatus() {
        //Given
        var payment = aPaymentWithPaymentStatus(null);
        //When
        Set<ConstraintViolation<Payment>> violations = validator.validate(payment);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentStatus");
    }
}