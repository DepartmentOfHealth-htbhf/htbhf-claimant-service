package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PregnancyEntitlementCalculatorTest {

    private static final int PREGNANCY_GRACE_PERIOD_IN_DAYS = 14;

    PregnancyEntitlementCalculator calculator = new PregnancyEntitlementCalculator(PREGNANCY_GRACE_PERIOD_IN_DAYS);

    @Test
    void shouldReturnTrueForDueDateInFuture() {
        boolean result = calculator.isEntitledToVoucher(LocalDate.now().plusDays(1));
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateToday() {
        boolean result = calculator.isEntitledToVoucher(LocalDate.now());
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateLessThanGracePeriodDaysAgo() {
        boolean result = calculator.isEntitledToVoucher(LocalDate.now().minusDays(1));
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateGracePeriodDaysAgo() {
        boolean result = calculator.isEntitledToVoucher(LocalDate.now().minusDays(PREGNANCY_GRACE_PERIOD_IN_DAYS));
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForNullDueDate() {
        boolean result = calculator.isEntitledToVoucher(null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForDueDateMoreThanGracePeriodDaysAgo() {
        boolean result = calculator.isEntitledToVoucher(LocalDate.now().minusDays(PREGNANCY_GRACE_PERIOD_IN_DAYS + 1));
        assertThat(result).isFalse();
    }
}