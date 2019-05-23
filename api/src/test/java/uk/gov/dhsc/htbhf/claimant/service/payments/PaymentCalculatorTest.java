package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers;

class PaymentCalculatorTest {

    private int maximumBalancePeriod = 8;
    private PaymentCalculator paymentCalculator = new PaymentCalculator(maximumBalancePeriod);

    @Test
    void shouldReturnFullPaymentAmountForLowBalance() {
        // Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
        int firstWeekVoucherEntitlementAmount = entitlement.getFirstVoucherEntitlementForCycle().getTotalVoucherValueInPence();
        int lowBalance = 1;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentAmountCycleInPence(entitlement, lowBalance);

        // Then
        assertThat(paymentAmount).isEqualTo(firstWeekVoucherEntitlementAmount * 4);
    }

    @Test
    void shouldReturnPartialPaymentAmountForBalanceBetweenFourAndEightWeeksEntitlement() {
        // Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
        int balance = 7500;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentAmountCycleInPence(entitlement, balance);

        // Then
        assertThat(paymentAmount).isEqualTo(2420);
    }

    @Test
    void shouldReturnNoPaymentAmountForHighBalance() {
        // Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
        int highBalance = 100000;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentAmountCycleInPence(entitlement, highBalance);

        // Then
        assertThat(paymentAmount).isEqualTo(0);
    }

    @Test
    void shouldReturnFullPaymentWhenBalancePlusFullPaymentIsEqualToMaximumCardBalanceThreshold() {
        // Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
        int balance = 4960;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentAmountCycleInPence(entitlement, balance);

        // Then
        assertThat(paymentAmount).isEqualTo(4960);
    }

    @Test
    void shouldReturnNoPaymentWhenCardBalanceIsEqualToMaximumCardBalanceThreshold() {
        // Given
        PaymentCycleVoucherEntitlement entitlement = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
        int balance = 9920;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentAmountCycleInPence(entitlement, balance);

        // Then
        assertThat(paymentAmount).isEqualTo(0);
    }
}