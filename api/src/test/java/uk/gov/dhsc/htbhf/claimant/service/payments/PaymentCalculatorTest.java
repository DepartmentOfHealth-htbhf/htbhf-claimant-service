package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers;

class PaymentCalculatorTest {

    private static final int MAXIMUM_BALANCE_PERIOD = 8;
    private PaymentCalculator paymentCalculator = new PaymentCalculator(MAXIMUM_BALANCE_PERIOD);
    private static final PaymentCycleVoucherEntitlement ENTITLEMENT = aPaymentCycleVoucherEntitlementWithFourWeeklyVouchers();
    private static final int MAXIMUM_BALANCE = 9920;
    private static final int PAYMENT_CYCLE_ENTITLEMENT_AMOUNT = 4960;

    @Test
    void shouldReturnFullPaymentAmountForLowBalance() {
        // Given
        int lowBalance = 1;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, lowBalance);

        // Then
        assertThat(paymentAmount).isEqualTo(PAYMENT_CYCLE_ENTITLEMENT_AMOUNT);
    }

    @Test
    void shouldReturnPartialPaymentAmountForBalanceBetweenFourAndEightWeeksEntitlement() {
        // Given
        int balance = 7500;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        int partialPayment = MAXIMUM_BALANCE - balance;
        assertThat(paymentAmount).isEqualTo(partialPayment);
    }

    @Test
    void shouldReturnNoPaymentAmountForHighBalance() {
        // Given
        int highBalance = 100000;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, highBalance);

        // Then
        assertThat(paymentAmount).isEqualTo(0);
    }

    @Test
    void shouldReturnFullPaymentWhenBalancePlusFullPaymentIsEqualToMaximumCardBalanceThreshold() {
        // Given
        int balance = PAYMENT_CYCLE_ENTITLEMENT_AMOUNT;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        assertThat(paymentAmount).isEqualTo(PAYMENT_CYCLE_ENTITLEMENT_AMOUNT);
    }

    @Test
    void shouldReturnNoPaymentWhenCardBalanceIsEqualToMaximumCardBalanceThreshold() {
        // Given
        int balance = MAXIMUM_BALANCE;

        // When
        int paymentAmount = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        assertThat(paymentAmount).isEqualTo(0);
    }
}