package uk.gov.dhsc.htbhf.claimant.entity;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.*;

public class PaymentCycleTest extends AbstractValidationTest {

    @Test
    void shouldValidatePaymentCycleSuccessfully() {
        //Given
        var paymentCycle = aValidPaymentCycle();
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoClaim() {
        //Given
        var paymentCycle = aPaymentCycleWithClaim(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claim");
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoCardAccountId() {
        //Given
        var paymentCycle = aPaymentCycleWithCardAccountId(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "cardAccountId");
    }
}
