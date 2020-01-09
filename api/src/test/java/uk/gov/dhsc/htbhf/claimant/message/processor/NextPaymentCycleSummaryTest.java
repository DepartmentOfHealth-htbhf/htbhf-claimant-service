package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary.NO_CHILDREN;

class NextPaymentCycleSummaryTest {

    @ParameterizedTest
    @CsvSource({"0, false",
            "1, true",
            "2, true",
            "15, true"})
    void testHasChildrenTurningFour(int childrenTurningFour, boolean expected) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(childrenTurningFour).build();
        assertThat(summary.hasChildrenTurningFour()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, false",
            "1, false",
            "2, true",
            "15, true"})
    void testHasMultipleChildrenTurningFour(int childrenTurningFour, boolean expected) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder().numberOfChildrenTurningFour(childrenTurningFour).build();
        assertThat(summary.hasMultipleChildrenTurningFour()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, false",
            "1, true",
            "2, true",
            "15, true"})
    void testHasChildrenTurningOne(int childrenTurningOne, boolean expected) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(childrenTurningOne).build();
        assertThat(summary.hasChildrenTurningOne()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, false",
            "1, false",
            "2, true",
            "15, true"})
    void testHasMultipleChildrenTurningOne(int childrenTurningOne, boolean expected) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder().numberOfChildrenTurningOne(childrenTurningOne).build();
        assertThat(summary.hasMultipleChildrenTurningOne()).isEqualTo(expected);
    }

    @Test
    void testCheckNoChildren() {
        assertThat(NO_CHILDREN.getNumberOfChildrenTurningFour()).isZero();
        assertThat(NO_CHILDREN.getNumberOfChildrenTurningOne()).isZero();
        assertThat(NO_CHILDREN.hasMultipleChildrenTurningOne()).isFalse();
        assertThat(NO_CHILDREN.hasMultipleChildrenTurningFour()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "1, 1, 2",
            "3, 2, 5"
    })
    void testNumberOfChildrenTurningOneOrFour(int childrenTuringOne, int childrenTurningFour, int total) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder()
                .numberOfChildrenTurningOne(childrenTuringOne)
                .numberOfChildrenTurningFour(childrenTurningFour)
                .build();

        assertThat(summary.getNumberOfChildrenTurningOneOrFour()).isEqualTo(total);
    }

    @ParameterizedTest
    @CsvSource({"0, true, false",
            "1, true, false",
            "1, false, true",
            "2, false, true",
            "0, false, false"})
    void shouldReportLastChildTurningFour(int childrenTurningFour, boolean childrenUnderFourPresentAtEndOfCycle, boolean expected) {
        NextPaymentCycleSummary summary = NextPaymentCycleSummary.builder()
                .numberOfChildrenTurningFour(childrenTurningFour)
                .childrenUnderFourPresentAtEndOfCycle(childrenUnderFourPresentAtEndOfCycle)
                .build();
        assertThat(summary.youngestChildTurnsFour()).isEqualTo(expected);
    }

}
