package uk.gov.dhsc.htbhf.claimant.entitlement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.dwp.model.QualifyingReason;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithStartAndEndDateAndClaim;

class PregnancyEntitlementCalculatorTest {

    private static final int PREGNANCY_GRACE_PERIOD_IN_WEEKS = 12;
    private static final int PAYMENT_CYCLE_DURATION_IN_DAYS = 28;
    private static final int UNDER_EIGHTEEN_PREGNANCY_GRACE_PERIOD_IN_WEEKS = 4;

    private PregnancyEntitlementCalculator calculator = new PregnancyEntitlementCalculator(
            PREGNANCY_GRACE_PERIOD_IN_WEEKS,
            PAYMENT_CYCLE_DURATION_IN_DAYS,
            UNDER_EIGHTEEN_PREGNANCY_GRACE_PERIOD_IN_WEEKS);

    @ParameterizedTest
    @MethodSource("isEntitledToVoucherArguments")
    void shouldReturnTrueWhenClaimantIsEntitledToVoucher(LocalDate entitlementDate, LocalDate dueDate) {
        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate, null);

        assertThat(result).isTrue();
    }

    private static Stream<Arguments> isEntitledToVoucherArguments() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(today, today.plusWeeks(1)),
                Arguments.of(today, today),
                Arguments.of(today, today.minusWeeks(1)),
                Arguments.of(today, today.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS))
        );
    }

    @ParameterizedTest
    @MethodSource("isUnder18PregnantEntitledToVoucherArguments")
    void shouldReturnTrueWhenUnder18PregnantClaimantIsEntitledToVoucher(LocalDate entitlementDate, LocalDate dueDate) {
        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate, QualifyingReason.UNDER_18);
        assertThat(result).isTrue();
    }

    private static Stream<Arguments> isUnder18PregnantEntitledToVoucherArguments() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(today, today.plusWeeks(1)),
                Arguments.of(today, today),
                Arguments.of(today, today.minusWeeks(1)),
                Arguments.of(today, today.minusWeeks(UNDER_EIGHTEEN_PREGNANCY_GRACE_PERIOD_IN_WEEKS))
        );
    }

    @Test
    void shouldReturnFalseForDueDateMoreThanGracePeriodWeeksAgo() {
        LocalDate entitlementDate = LocalDate.now();
        LocalDate dueDate = entitlementDate.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS + 1);

        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate, null);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForNullDueDate() {
        boolean result = calculator.isEntitledToVoucher(null, LocalDate.now(), null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenEntitlementDateIsNull() {
        IllegalArgumentException thrown = catchThrowableOfType(() ->
                calculator.isEntitledToVoucher(LocalDate.now(), null, null), IllegalArgumentException.class);

        assertThat(thrown.getMessage()).isEqualTo("entitlementDate must not be null");
    }

    @Test
    void shouldReturnTrueWhenPaymentCycleIsSecondToLastOneWithPregnancyVouchers() {
        // The claimant will receive pregnancy vouchers for this cycle and the one after but not after that (given a 12 week grace period and four week cycles).
        LocalDate expectedDeliveryDate = LocalDate.now().minusWeeks(5);
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);

        boolean result = calculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(paymentCycle);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("expectedDeliveryDatesNotRequiringReminderEmail")
    void shouldReturnFalseWhenPaymentCycleIsNotSecondToLastOneWithPregnancyVouchers(LocalDate expectedDeliveryDate) {
        Claim claim = aClaimWithExpectedDeliveryDate(expectedDeliveryDate);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);

        boolean result = calculator.currentCycleIsSecondToLastCycleWithPregnancyVouchers(paymentCycle);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @MethodSource("isPregnantInPaymentCycle")
    void shouldReturnTrueWhenClaimantIsPregnantInPaymentCycle(LocalDate entitlementDate, LocalDate dueDate) {
        boolean result = calculator.isEntitledToVoucher(dueDate, entitlementDate, null);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnClaimantIsNotPregnantInCycleForNullDueDate() {
        Claim claim = aClaimWithExpectedDeliveryDate(null);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);

        boolean result = calculator.claimantIsPregnantInCycle(paymentCycle);

        assertThat(result).isFalse();
    }

    private static Stream<Arguments> isPregnantInPaymentCycle() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(today, today.plusWeeks(1)),
                Arguments.of(today, today),
                Arguments.of(today, today.minusWeeks(1)),
                Arguments.of(today, today.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS))
        );
    }

    @ParameterizedTest
    @MethodSource("isPregnantOnDayAfterPaymentCycle")
    void shouldReturnTrueWhenClaimantIsPregnantAfterPaymentCycle(PaymentCycle paymentCycle) {
        boolean result = calculator.claimantIsPregnantAfterCycle(paymentCycle);

        assertThat(result).isTrue();
    }

    private static Stream<Arguments> isPregnantOnDayAfterPaymentCycle() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusWeeks(4);
        return Stream.of(
                Arguments.of(aPaymentCycleWithStartAndEndDateAndClaim(startDate, today, aClaimWithExpectedDeliveryDate(today))),
                Arguments.of(aPaymentCycleWithStartAndEndDateAndClaim(startDate, today,
                        aClaimWithExpectedDeliveryDate(today.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS).plusDays(1)))),
                Arguments.of(aPaymentCycleWithStartAndEndDateAndClaim(startDate, today, aClaimWithExpectedDeliveryDate(today.minusDays(1))))
        );
    }

    @ParameterizedTest
    @MethodSource("isNotPregnantOnDayAfterPaymentCycle")
    void shouldReturnFalseWhenClaimantIsNotPregnantAfterPaymentCycle(PaymentCycle paymentCycle) {
        boolean result = calculator.claimantIsPregnantAfterCycle(paymentCycle);

        assertThat(result).isFalse();
    }

    private static Stream<Arguments> isNotPregnantOnDayAfterPaymentCycle() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusWeeks(4);
        return Stream.of(
                Arguments.of(aPaymentCycleWithStartAndEndDateAndClaim(startDate, today, aClaimWithExpectedDeliveryDate(null))),
                Arguments.of(aPaymentCycleWithStartAndEndDateAndClaim(startDate, today,
                        aClaimWithExpectedDeliveryDate(today.minusWeeks(PREGNANCY_GRACE_PERIOD_IN_WEEKS))))
        );
    }

    private static Stream<Arguments> expectedDeliveryDatesNotRequiringReminderEmail() {
        return Stream.of(
                Arguments.of(LocalDate.now().minusWeeks(3)), // current payment cycle is last cycle with pregnancy vouchers
                Arguments.of(LocalDate.now().plusWeeks(7)), // current payment cycle is third to last cycle with vouchers
                Arguments.of(LocalDate.now().plusWeeks(16)), // will receive pregnancy vouchers for seven more cycles
                Arguments.of(LocalDate.now().minusWeeks(16)) // due date is too far in the past, will receive no pregnancy vouchers
        );
    }
}
