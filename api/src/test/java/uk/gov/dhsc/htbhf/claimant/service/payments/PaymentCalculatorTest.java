package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.PARTIAL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

class PaymentCalculatorTest {

    private static final int MAXIMUM_BALANCE_PERIOD = 8;
    private PaymentCalculator paymentCalculator = new PaymentCalculator(MAXIMUM_BALANCE_PERIOD);
    private static final PaymentCycleVoucherEntitlement ENTITLEMENT = aPaymentCycleVoucherEntitlementWithVouchers();
    private static final int MAXIMUM_BALANCE = 9920;
    private static final int PAYMENT_CYCLE_ENTITLEMENT_AMOUNT = 4960;

    @Test
    void shouldReturnFullPaymentAmountForLowBalance() {
        // Given
        int lowBalance = 1;

        // When
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, lowBalance);

        // Then
        assertThat(paymentCalculation.getPaymentAmount()).isEqualTo(PAYMENT_CYCLE_ENTITLEMENT_AMOUNT);
        assertThat(paymentCalculation.getPaymentCycleStatus()).isEqualTo(FULL_PAYMENT_MADE);
        assertThat(paymentCalculation.getAvailableBalanceInPence()).isEqualTo(lowBalance);
    }

    @Test
    void shouldReturnPartialPaymentAmountForBalanceBetweenFourAndEightWeeksEntitlement() {
        // Given
        int balance = 7500;

        // When
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        int partialPayment = MAXIMUM_BALANCE - balance;
        assertThat(paymentCalculation.getPaymentAmount()).isEqualTo(partialPayment);
        assertThat(paymentCalculation.getPaymentCycleStatus()).isEqualTo(PARTIAL_PAYMENT_MADE);
        assertThat(paymentCalculation.getAvailableBalanceInPence()).isEqualTo(balance);
    }

    @Test
    void shouldReturnNoPaymentAmountForHighBalance() {
        // Given
        int highBalance = 100000;

        // When
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, highBalance);

        // Then
        assertThat(paymentCalculation.getPaymentAmount()).isEqualTo(0);
        assertThat(paymentCalculation.getPaymentCycleStatus()).isEqualTo(BALANCE_TOO_HIGH_FOR_PAYMENT);
        assertThat(paymentCalculation.getAvailableBalanceInPence()).isEqualTo(highBalance);
    }

    @Test
    void shouldReturnFullPaymentWhenBalancePlusFullPaymentIsEqualToMaximumCardBalanceThreshold() {
        // Given
        int balance = PAYMENT_CYCLE_ENTITLEMENT_AMOUNT;

        // When
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        assertThat(paymentCalculation.getPaymentAmount()).isEqualTo(PAYMENT_CYCLE_ENTITLEMENT_AMOUNT);
        assertThat(paymentCalculation.getPaymentCycleStatus()).isEqualTo(FULL_PAYMENT_MADE);
        assertThat(paymentCalculation.getAvailableBalanceInPence()).isEqualTo(balance);
    }

    @Test
    void shouldReturnNoPaymentWhenCardBalanceIsEqualToMaximumCardBalanceThreshold() {
        // Given
        int balance = MAXIMUM_BALANCE;

        // When
        PaymentCalculation paymentCalculation = paymentCalculator.calculatePaymentCycleAmountInPence(ENTITLEMENT, balance);

        // Then
        assertThat(paymentCalculation.getPaymentAmount()).isEqualTo(0);
        assertThat(paymentCalculation.getPaymentCycleStatus()).isEqualTo(BALANCE_TOO_HIGH_FOR_PAYMENT);
        assertThat(paymentCalculation.getAvailableBalanceInPence()).isEqualTo(balance);
    }
}
