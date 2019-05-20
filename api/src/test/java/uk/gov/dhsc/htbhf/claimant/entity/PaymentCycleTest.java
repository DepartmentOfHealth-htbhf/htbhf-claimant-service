package uk.gov.dhsc.htbhf.claimant.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.assertions.AbstractValidationTest;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import java.util.Set;
import javax.validation.ConstraintViolation;

import static uk.gov.dhsc.htbhf.assertions.ConstraintViolationAssert.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
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
}
