package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithChildrenDobs;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPregnancyVouchersOnly;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class ChildDateOfBirthCalculatorTest {

    private static final LocalDate YOUNGEST_CHILD_DOB = LocalDate.now().minusMonths(6);
    private static final LocalDate ELDEST_CHILD_DOB = LocalDate.now().minusYears(3).minusMonths(6);
    private static final LocalDate DOB_TURNS_ONE_IN_NEXT_PAYMENT_CYCLE = LocalDate.now().minusYears(1).plusWeeks(5);
    private static final LocalDate DOB_TURNS_FOUR_IN_NEXT_PAYMENT_CYCLE = LocalDate.now().minusYears(4).plusWeeks(5);
    private static final LocalDate CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE = LocalDate.now().plusWeeks(3);
    private static final LocalDate NEXT_CYCLE_FINAL_ENTITLEMENT_DATE = LocalDate.now().plusWeeks(7);

    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    @InjectMocks
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    @BeforeEach
    void setupMocks() {
        lenient().when(paymentCycleEntitlementCalculator.getVoucherEntitlementDatesFromStartDate(LocalDate.now()))
                .thenReturn(List.of(
                        LocalDate.now(),
                        LocalDate.now().plusWeeks(1),
                        LocalDate.now().plusWeeks(2),
                        CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE
                ));
        lenient().when(paymentCycleEntitlementCalculator.getVoucherEntitlementDatesFromStartDate(LocalDate.now().plusWeeks(4)))
                .thenReturn(List.of(
                        LocalDate.now().plusWeeks(4),
                        LocalDate.now().plusWeeks(5),
                        LocalDate.now().plusWeeks(6),
                        NEXT_CYCLE_FINAL_ENTITLEMENT_DATE
                ));
    }

    @Test
    void shouldReturnNoChildrenTurningFourInPaymentCycle() {
        //Given standard children DOBs in PaymentCycle are nowhere near triggering in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnOneChildrenTurningFourThatAffectsNextPayment() {
        //Given eldest child will turn 4 in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                DOB_TURNS_FOUR_IN_NEXT_PAYMENT_CYCLE
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnOneChildrenTurningFourInPaymentCycleWithBirthdayOnEndBoundary() {
        //Given eldest child will turn 4 on the final day of the last entitlement period of the next PaymentCycle.
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                NEXT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(4)
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnNoChildrenTurningFourInPaymentCycleWithBirthdayAfterEndBoundary() {
        //Given eldest child will turn 4 just after the final day of the last entitlement period of the next PaymentCycle.
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                NEXT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(4).plusDays(1)
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnNoChildrenTurningFourInPaymentCycleWithBirthdayOnStartBoundary() {
        //Given eldest child will turn 4 on the start of the last entitlement date for the current PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(4)
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnOneChildrenTurningFourInPaymentCycleWithBirthdayJustAfterStartBoundary() {
        //Given eldest child will turn 4 the day after the start of the last entitlement date for the current PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(4).plusDays(1)
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnTwoChildrenTurningFourInPaymentCycle() {
        //Given twins will turn 4 in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                DOB_TURNS_FOUR_IN_NEXT_PAYMENT_CYCLE,
                DOB_TURNS_FOUR_IN_NEXT_PAYMENT_CYCLE
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(2).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnNoChildrenTurningFourInPaymentCycleForPregnantWomanWithNoChildren() {
        //Given a PaymentCycle with no children dobs in it
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(LocalDate.now(), LocalDate.now().plusWeeks(4));
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnNoChildrenTurningOneInPaymentCycle() {
        //Given standard children DOBs in PaymentCycle are nowhere near triggering in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                YOUNGEST_CHILD_DOB,
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnOneChildrenTurningOneThatAffectsNextPayment() {
        //Given youngest child will turn 1 in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                DOB_TURNS_ONE_IN_NEXT_PAYMENT_CYCLE,
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnOneChildrenTurningOneInPaymentCycleWithBirthdayOnEndBoundary() {
        //Given youngest child will turn 1 on the final day of the last entitlement period of the next PaymentCycle.
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                NEXT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(1),
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnNoChildrenTurningOneInPaymentCycleWithBirthdayAfterEndBoundary() {
        //Given youngest child will turn 1 just after the final day of the last entitlement period of the next PaymentCycle.
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                NEXT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(1).plusDays(1),
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnNoChildrenTurningOneInPaymentCycleWithBirthdayOnStartBoundary() {
        //Given youngest child will turn 1 on the start of the last entitlement date for the current PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(1),
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnOneChildrenTurningOneInPaymentCycleWithBirthdayJustAfterStartBoundary() {
        //Given youngest child will turn 1 the day after the start of the last entitlement date for the current PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                CURRENT_CYCLE_FINAL_ENTITLEMENT_DATE.minusYears(1).plusDays(1),
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnTwoChildrenTurningOneInPaymentCycle() {
        //Given twins will turn 1 in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                DOB_TURNS_ONE_IN_NEXT_PAYMENT_CYCLE,
                DOB_TURNS_ONE_IN_NEXT_PAYMENT_CYCLE,
                ELDEST_CHILD_DOB
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(2).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @Test
    void shouldReturnNoChildrenTurningOneInPaymentCycleForPregnantWomanWithNoChildren() {
        //Given a PaymentCycle with no children dobs in it
        PaymentCycle paymentCycle = aPaymentCycleWithPregnancyVouchersOnly(LocalDate.now(), LocalDate.now().plusWeeks(4));
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        assertThat(summary).isEqualTo(NO_CHILDREN);
    }

    @Test
    void shouldReturnChildrenTurningOneAndChildTurningFourInPaymentCycle() {
        //Given one child will turn 1 and another will turn 4 in the next PaymentCycle
        PaymentCycle paymentCycle = buildPaymentCycleWithChildDobs(
                DOB_TURNS_ONE_IN_NEXT_PAYMENT_CYCLE,
                DOB_TURNS_FOUR_IN_NEXT_PAYMENT_CYCLE
        );
        //When
        NextPaymentCycleSummary summary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        //Then
        NextPaymentCycleSummary expectedSummary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(1).numberOfChildrenTurningFour(1).build();
        assertThat(summary).isEqualTo(expectedSummary);
    }

    @ParameterizedTest(name = "Children dobs={0}")
    @MethodSource("provideArgumentsForChildrenUnderFour")
    void shouldReturnHadChildrenAtStartOfPaymentCycle(List<LocalDate> childrenDobs) {
        //Given a children under 4 in the PaymentCycle
        PaymentCycle paymentCycle = aPaymentCycleWithChildrenDobs(childrenDobs);
        //When
        boolean hasChildren = childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(paymentCycle);
        //Then
        assertThat(hasChildren).isTrue();
    }

    @ParameterizedTest(name = "Children dobs={0}")
    @MethodSource("provideArgumentsForChildrenFourAndOver")
    void shouldReturnHadNoChildrenAtStartOfPaymentCycle(List<LocalDate> childrenDobs) {
        //Given a children 4 or over 4 in the PaymentCycle
        PaymentCycle paymentCycle = aPaymentCycleWithChildrenDobs(childrenDobs);
        //When
        boolean hasChildren = childDateOfBirthCalculator.hadChildrenUnder4AtStartOfPaymentCycle(paymentCycle);
        //Then
        assertThat(hasChildren).isFalse();
    }

    @ParameterizedTest(name = "Children dobs={0}")
    @MethodSource("provideArgumentsForChildrenUnderFour")
    void shouldReturnHadChildrenForGivenDate(List<LocalDate> childrenDobs) {
        //When
        boolean hasChildren = childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(childrenDobs, LocalDate.now());
        //Then
        assertThat(hasChildren).isTrue();
    }

    @ParameterizedTest(name = "Children dobs={0}")
    @MethodSource("provideArgumentsForChildrenFourAndOver")
    void shouldReturnHadNoChildrenForGivenDate(List<LocalDate> childrenDobs) {
        //When
        boolean hasChildren = childDateOfBirthCalculator.hadChildrenUnderFourAtGivenDate(childrenDobs, LocalDate.now());
        //Then
        assertThat(hasChildren).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForNumberOfChildrenUnderOne")
    void shouldGetNumberOfChildrenUnderOne(List<LocalDate> childrenDobs, int expectedNumberOfChildrenUnderOne) {
        //When
        int numberOfChildrenUnderOne = ChildDateOfBirthCalculator.getNumberOfChildrenUnderOne(childrenDobs, LocalDate.now());
        //Then
        assertThat(numberOfChildrenUnderOne).isEqualTo(expectedNumberOfChildrenUnderOne);
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForNumberOfChildrenUnderFour")
    void shouldGetNumberOfChildrenUnderFour(List<LocalDate> childrenDobs, int expectedNumberOfChildrenUnderFour) {
        //When
        int numberOfChildrenUnderFour = ChildDateOfBirthCalculator.getNumberOfChildrenUnderFour(childrenDobs, LocalDate.now());
        //Then
        assertThat(numberOfChildrenUnderFour).isEqualTo(expectedNumberOfChildrenUnderFour);
    }

    private static Stream<Arguments> provideArgumentsForNumberOfChildrenUnderOne() {
        return Stream.of(
                Arguments.of(emptyList(), 0),
                Arguments.of(null, 0),
                Arguments.of(SINGLE_THREE_YEAR_OLD, 0),
                Arguments.of(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, 1),
                Arguments.of(TWO_CHILDREN_UNDER_ONE, 2)
        );
    }

    private static Stream<Arguments> provideArgumentsForNumberOfChildrenUnderFour() {
        return Stream.of(
                Arguments.of(emptyList(), 0),
                Arguments.of(null, 0),
                Arguments.of(SINGLE_FIVE_YEAR_OLD, 0),
                Arguments.of(SINGLE_THREE_YEAR_OLD, 1),
                Arguments.of(TWO_CHILDREN_BETWEEN_ONE_AND_FOUR, 2)
        );
    }

    private static Stream<Arguments> provideArgumentsForChildrenFourAndOver() {
        return Stream.of(
                Arguments.of(emptyList()),
                Arguments.of(List.of(LocalDate.now().minusYears(4))),
                Arguments.of(List.of(LocalDate.now().minusYears(4).minusDays(1))),
                Arguments.of(List.of(LocalDate.now().minusYears(5), LocalDate.now().minusYears(6))),
                Arguments.of(List.of(LocalDate.now().minusYears(4).minusMonths(6)))
        );
    }

    private static Stream<Arguments> provideArgumentsForChildrenUnderFour() {
        return Stream.of(
                Arguments.of(List.of(YOUNGEST_CHILD_DOB)),
                Arguments.of(List.of(ELDEST_CHILD_DOB)),
                Arguments.of(List.of(LocalDate.now().minusYears(4).plusDays(1))),
                Arguments.of(List.of(YOUNGEST_CHILD_DOB, ELDEST_CHILD_DOB))
        );
    }

    private PaymentCycle buildPaymentCycleWithChildDobs(LocalDate... childDobs) {
        return aPaymentCycleWithChildrenDobs(List.of(childDobs));
    }

}
