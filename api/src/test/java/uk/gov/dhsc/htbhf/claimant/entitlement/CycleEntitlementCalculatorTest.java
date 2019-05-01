package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class CycleEntitlementCalculatorTest {

    private static final int PAYMENT_CYCLE_DURATION_IN_DAYS = 3;
    private static final int NUMBER_OF_CALCULATION_PERIODS = 3;
    private static final int NUMBER_OF_WEEKS_BEFORE_PREGNANCY = 16;
    private static final int NUMBER_OF_WEEKS_AFTER_PREGNANCY = 8;
    private static final int NUMBER_OF_CALCULATIONS_PER_CYCLE = 3;

    private EntitlementCalculator entitlementCalculator = mock(EntitlementCalculator.class);

    private CycleEntitlementCalculator cycleEntitlementCalculator = new CycleEntitlementCalculator(
            PAYMENT_CYCLE_DURATION_IN_DAYS,
            NUMBER_OF_CALCULATION_PERIODS,
            NUMBER_OF_WEEKS_BEFORE_PREGNANCY,
            NUMBER_OF_WEEKS_AFTER_PREGNANCY,
            entitlementCalculator);

    @Test
    void shouldThrowExceptionWhenDurationIsNotDivisibleByNumberOfCalculationPeriods() {
        // 10 is not divisible by 3, so should throw an exception
        int paymentCycleDurationInDays = 10;
        int numberOfCalculationPeriods = 3;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new CycleEntitlementCalculator(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16, entitlementCalculator),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Payment cycle duration of 10 days is not divisible by number of calculation periods 3");
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZero() {
        int paymentCycleDurationInDays = 0;
        int numberOfCalculationPeriods = 1;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new CycleEntitlementCalculator(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16, entitlementCalculator),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Payment cycle duration can not be zero");
    }

    @Test
    void shouldThrowExceptionWhenNumberOrCalculationPeriodsIsZero() {
        int paymentCycleDurationInDays = 1;
        int numberOfCalculationPeriods = 0;
        IllegalArgumentException thrown = catchThrowableOfType(
                () -> new CycleEntitlementCalculator(paymentCycleDurationInDays, numberOfCalculationPeriods, 8, 16, entitlementCalculator),
                IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("Number of calculation periods can not be zero");
    }

    @Test
    void shouldCallEntitlementCalculatorForEachEntitlementDate() {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        List<LocalDate> dateOfBirthsOfChildren = singletonList(LocalDate.now().minusMonths(6));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().plusMonths(1));

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren);

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(expectedDueDate, dateOfBirthsOfChildren);
    }

    @ParameterizedTest
    @ValueSource(ints = {-NUMBER_OF_WEEKS_BEFORE_PREGNANCY, 2, NUMBER_OF_WEEKS_AFTER_PREGNANCY})
    void shouldCallEntitlementCalculatorWithEmptyDueDateWhenNewChildMatchedToPregnancy(Integer numberOfWeeksDobIsAfterDueDate) {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        PaymentCycleVoucherEntitlement previousEntitlement = createPaymentEntitlementWithPregnancyVouchers(1);
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now());
        List<LocalDate> dateOfBirthsOfChildren = singletonList(expectedDueDate.get().plusWeeks(numberOfWeeksDobIsAfterDueDate));

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, previousEntitlement);

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(Optional.empty(), dateOfBirthsOfChildren);
    }

    @Test
    void shouldCallEntitlementCalculatorWithExpectedDueDateWhenNoNewChildrenMatchedToPregnancy() {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        PaymentCycleVoucherEntitlement previousEntitlement = createPaymentEntitlementWithPregnancyVouchers(1);
        // child's dob falls outside the date range in which we consider a child to be a result of the current pregnancy
        List<LocalDate> dateOfBirthsOfChildren = singletonList(LocalDate.now().minusMonths(8));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(1));

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, previousEntitlement);

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(expectedDueDate, dateOfBirthsOfChildren);
    }

    @Test
    void shouldCallEntitlementCalculatorWithExpectedDueDateWhenNoPreviousPregnancyVouchers() {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        PaymentCycleVoucherEntitlement previousEntitlement = createPaymentEntitlementWithPregnancyVouchers(0);
        List<LocalDate> dateOfBirthsOfChildren = singletonList(LocalDate.now().minusMonths(8));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().plusMonths(8));

        PaymentCycleVoucherEntitlement result = cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, previousEntitlement);

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(expectedDueDate, dateOfBirthsOfChildren);
    }

    private void assertEntitlement(VoucherEntitlement voucherEntitlement, PaymentCycleVoucherEntitlement result) {
        PaymentCycleVoucherEntitlement expected = new PaymentCycleVoucherEntitlement(nCopies(NUMBER_OF_CALCULATIONS_PER_CYCLE, voucherEntitlement));
        assertThat(result).isEqualTo(expected);
    }

    private PaymentCycleVoucherEntitlement createPaymentEntitlementWithPregnancyVouchers(Integer pregnancyVouchers) {
        VoucherEntitlement previousVoucherEntitlement = VoucherEntitlement.builder().vouchersForPregnancy(pregnancyVouchers).build();
        return new PaymentCycleVoucherEntitlement(singletonList(previousVoucherEntitlement));
    }

    private void verifyEntitlementCalculatorCalled(Optional<LocalDate> expectedDueDate, List<LocalDate> dateOfBirthsOfChildren) {
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now());
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now().plusDays(1));
        verify(entitlementCalculator).calculateVoucherEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now().plusDays(2));
    }
}
