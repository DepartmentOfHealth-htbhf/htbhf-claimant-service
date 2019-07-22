package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PregnancyEntitlementCalculatorTest {

    private static final int PREGNANCY_GRACE_PERIOD_IN_WEEKS = 12;

    private PregnancyEntitlementCalculator calculator = new PregnancyEntitlementCalculator(PREGNANCY_GRACE_PERIOD_IN_WEEKS);

    @Test
    void shouldReturnTrueForDueDateInFuture() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.plusWeeks(1);

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
    void shouldReturnTrueForDueDateLessThanGracePeriodWeeksAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusWeeks(1);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDueDateGracePeriodWeeksAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForDueDateMoreThanGracePeriodWeeksAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS + 1);

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
