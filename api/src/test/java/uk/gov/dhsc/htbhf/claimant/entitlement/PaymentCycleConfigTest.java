package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PaymentCycleConfigTest {

    @Test
    void shouldThrowExceptionWhenDurationIsNotDivisibleByNumberOfCalculationPeriods() {
        // 10 is not divisible by 3, so should throw an exception
        int paymentCycleDurationInDays = 10;
        int numberOfCalculationPeriods = 3;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new PaymentCycleConfig(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Payment cycle duration of 10 days is not divisible by number of calculation periods 3");
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZero() {
        int paymentCycleDurationInDays = 0;
        int numberOfCalculationPeriods = 1;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new PaymentCycleConfig(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Payment cycle duration must be greater than zero");
    }

    @Test
    void shouldThrowExceptionWhenDurationIsNegative() {
        int paymentCycleDurationInDays = -5;
        int numberOfCalculationPeriods = 1;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new PaymentCycleConfig(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Payment cycle duration must be greater than zero");
    }

    @Test
    void shouldThrowExceptionWhenNumberOrCalculationPeriodsIsZero() {
        int paymentCycleDurationInDays = 1;
        int numberOfCalculationPeriods = 0;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new PaymentCycleConfig(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Number of calculation periods must be greater than zero");
    }

    @Test
    void shouldThrowExceptionWhenNumberOrCalculationPeriodsIsNegative() {
        int paymentCycleDurationInDays = 1;
        int numberOfCalculationPeriods = -5;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new PaymentCycleConfig(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Number of calculation periods must be greater than zero");
    }

}
