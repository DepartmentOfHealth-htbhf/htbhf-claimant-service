package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

public class PaymentCycleTest extends AbstractValidationTest {

    @Test
    void shouldValidatePaymentCycleSuccessfully() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycle();
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasNoViolations();
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoClaim() {
        //Given
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "claim");
    }

    @Test
    void shouldFailToValidatePaymentCycleWithNoStatus() {
        //Given
        PaymentCycle paymentCycle = aPaymentCycleWithStatus(null);
        //When
        Set<ConstraintViolation<PaymentCycle>> violations = validator.validate(paymentCycle);
        //Then
        assertThat(violations).hasSingleConstraintViolation("must not be null", "paymentCycleStatus");
    }

    @Test
    void shouldApplyVoucherEntitlement() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .voucherEntitlement(null)
                .totalEntitlementAmountInPence(null)
                .totalVouchers(null)
                .build();
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        Assertions.assertThat(entitlement.getTotalVoucherEntitlement()).isGreaterThan(0);
        Assertions.assertThat(entitlement.getTotalVoucherValueInPence()).isGreaterThan(0);
        //When
        paymentCycle.applyVoucherEntitlement(entitlement);
        //Then
        Assertions.assertThat(paymentCycle.getVoucherEntitlement()).isEqualTo(entitlement);
        Assertions.assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isEqualTo(entitlement.getTotalVoucherValueInPence());
        Assertions.assertThat(paymentCycle.getTotalVouchers()).isEqualTo(entitlement.getTotalVoucherEntitlement());
    }

    @Test
    void shouldApplyNullVoucherEntitlement() {
        //Given
        PaymentCycle paymentCycle = aValidPaymentCycle();
        //When
        paymentCycle.applyVoucherEntitlement(null);
        //Then
        Assertions.assertThat(paymentCycle.getVoucherEntitlement()).isNull();
        Assertions.assertThat(paymentCycle.getTotalEntitlementAmountInPence()).isNull();
        Assertions.assertThat(paymentCycle.getTotalVouchers()).isNull();
    }

    @Test
    void shouldAlwaysReturnAnIdFromGetId() {
        //Given
        PaymentCycle paymentCycle = PaymentCycle.builder().build();
        //When
        UUID id = paymentCycle.getId();
        //Then
        Assertions.assertThat(id).isNotNull();
    }

    @Test
    void shouldReturnTheSameIdIfOneIsSet() {
        //Given
        UUID id = UUID.randomUUID();
        //When
        PaymentCycle paymentCycle = PaymentCycle.builder().build();
        ReflectionTestUtils.setField(paymentCycle, "id", id);
        //Then
        Assertions.assertThat(id).isEqualTo(paymentCycle.getId());
    }
}
