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

    @Test
    void shouldFailToValidatePaymentCycleWithNoEligibilityStatus() {
        //Given
        var paymentCycle = aPaymentCycleWithEligibilityStatus(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "eligibilityStatus");
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoVoucherEntitlement() {
        //Given
        var paymentCycle = aPaymentCycleWithVoucherEntitlement(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "voucherEntitlement");
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoTotalVouchers() {
        //Given
        var paymentCycle = aPaymentCycleWithTotalVouchers(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "totalVouchers");
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoTotalEntitlementAmountInPence() {
        //Given
        var paymentCycle = aPaymentCycleWithTotalEntitlementAmountInPence(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "totalEntitlementAmountInPence");
    }
}
