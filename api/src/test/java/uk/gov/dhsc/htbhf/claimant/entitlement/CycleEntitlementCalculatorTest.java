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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.VoucherEntitlementTestDataFactory.aValidVoucherEntitlement;

@ExtendWith(MockitoExtension.class)
class CycleEntitlementCalculatorTest {

    private static final int PAYMENT_CYCLE_DURATION_IN_DAYS = 3;
    private static final int NUMBER_OF_WEEKS_BEFORE_PREGNANCY = 16;
    private static final int NUMBER_OF_WEEKS_AFTER_PREGNANCY = 8;
    private static final int NUMBER_OF_DAYS_BEFORE_PREGNANCY = 16 * 7;
    private static final int NUMBER_OF_DAYS_AFTER_PREGNANCY = 8 * 7;
    private static final int NUMBER_OF_CALCULATION_PERIODS = 3;

    private EntitlementCalculator entitlementCalculator = mock(EntitlementCalculator.class);
    private BackDatedCycleEntitlementCalculator backDatedCycleEntitlementCalculator = mock(BackDatedCycleEntitlementCalculator.class);
    private PaymentCycleConfig paymentCycleConfig = new PaymentCycleConfig(
            PAYMENT_CYCLE_DURATION_IN_DAYS,
            NUMBER_OF_CALCULATION_PERIODS,
            NUMBER_OF_WEEKS_BEFORE_PREGNANCY,
            NUMBER_OF_WEEKS_AFTER_PREGNANCY);

    private CycleEntitlementCalculator cycleEntitlementCalculator
            = new CycleEntitlementCalculator(paymentCycleConfig, entitlementCalculator, backDatedCycleEntitlementCalculator);

    @Test
    void shouldCallEntitlementCalculatorForEachEntitlementDate() {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        List<LocalDate> dateOfBirthsOfChildren = singletonList(LocalDate.now().minusMonths(6));
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().plusMonths(1));

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now());

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(expectedDueDate, dateOfBirthsOfChildren);
    }

    @ParameterizedTest
    @ValueSource(ints = {-NUMBER_OF_WEEKS_BEFORE_PREGNANCY, 2, NUMBER_OF_WEEKS_AFTER_PREGNANCY})
    void shouldCallEntitlementCalculatorWithEmptyDueDateAndGetBackDatedVouchersWhenNewChildMatchedToPregnancy(Integer numberOfWeeksDobIsAfterDueDate) {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        int backDatedVouchers = 2;
        LocalDate cycleStartDate = LocalDate.now();
        given(backDatedCycleEntitlementCalculator.calculateBackDatedVouchers(any(), anyList(), any())).willReturn(backDatedVouchers);
        PaymentCycleVoucherEntitlement previousEntitlement = createPaymentEntitlementWithPregnancyVouchers(1);
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now());
        List<LocalDate> dateOfBirthsOfChildren = singletonList(expectedDueDate.get().plusWeeks(numberOfWeeksDobIsAfterDueDate));

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, cycleStartDate, previousEntitlement);

        assertEntitlementWithBackDatedVouchers(backDatedVouchers, voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(Optional.empty(), dateOfBirthsOfChildren);
        verify(backDatedCycleEntitlementCalculator).calculateBackDatedVouchers(expectedDueDate, dateOfBirthsOfChildren, cycleStartDate);
    }

    // Children's dates of birth set to one day before and one day after the period where we match the child to the expected due date
    @ParameterizedTest
    @ValueSource(ints = {-(NUMBER_OF_DAYS_BEFORE_PREGNANCY + 1), NUMBER_OF_DAYS_AFTER_PREGNANCY + 1})
    void shouldCallEntitlementCalculatorWithExpectedDueDateWhenNoNewChildrenMatchedToPregnancy(Integer numberOfDaysDobIsAfterDueDate) {
        VoucherEntitlement voucherEntitlement = aValidVoucherEntitlement();
        given(entitlementCalculator.calculateVoucherEntitlement(any(), any(), any())).willReturn(voucherEntitlement);
        PaymentCycleVoucherEntitlement previousEntitlement = createPaymentEntitlementWithPregnancyVouchers(1);
        // child's dob falls outside the date range in which we consider a child to be a result of the current pregnancy
        Optional<LocalDate> expectedDueDate = Optional.of(LocalDate.now().minusWeeks(1));
        List<LocalDate> dateOfBirthsOfChildren = singletonList(expectedDueDate.get().plusDays(numberOfDaysDobIsAfterDueDate));

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now(), previousEntitlement);

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

        PaymentCycleVoucherEntitlement result =
                cycleEntitlementCalculator.calculateEntitlement(expectedDueDate, dateOfBirthsOfChildren, LocalDate.now(), previousEntitlement);

        assertEntitlement(voucherEntitlement, result);
        verifyEntitlementCalculatorCalled(expectedDueDate, dateOfBirthsOfChildren);
    }

    private void assertEntitlement(VoucherEntitlement voucherEntitlement, PaymentCycleVoucherEntitlement result) {
        assertEntitlementWithBackDatedVouchers(0, voucherEntitlement, result);
    }

    private void assertEntitlementWithBackDatedVouchers(int backDatedVouchers, VoucherEntitlement voucherEntitlement, PaymentCycleVoucherEntitlement result) {
        var expected = new PaymentCycleVoucherEntitlement(nCopies(NUMBER_OF_CALCULATION_PERIODS, voucherEntitlement), backDatedVouchers);
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
