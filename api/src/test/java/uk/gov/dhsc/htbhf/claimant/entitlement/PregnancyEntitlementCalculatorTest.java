package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PregnancyEntitlementCalculatorTest {

    private static final int PREGNANCY_GRACE_PERIOD_IN_DAYS = 14;

    PregnancyEntitlementCalculator calculator = new PregnancyEntitlementCalculator(PREGNANCY_GRACE_PERIOD_IN_DAYS);

    @Test
    void shouldReturnTrueForDueDateInFuture() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.plusDays(1);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateSameAsEntitlementDate() {
        LocalDate date = LocalDate.now();

        boolean result = calculator.isEntitledToVoucher(date, date);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateLessThanGracePeriodDaysAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusDays(1);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateGracePeriodDaysAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusDays(PREGNANCY_GRACE_PERIOD_IN_DAYS);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForDueDateMoreThanGracePeriodDaysAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusDays(PREGNANCY_GRACE_PERIOD_IN_DAYS + 1);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForNullDueDate() {
        boolean result = calculator.isEntitledToVoucher(null, LocalDate.now());
        assertThat(result).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenEntitlementDateIsNull() {
        IllegalArgumentException thrown = catchThrowableOfType(() -> calculator.isEntitledToVoucher(LocalDate.now(), null), IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("entitlementDate must not be null");
    }
}
