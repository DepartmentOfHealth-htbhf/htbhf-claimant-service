package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.Builder;
import lombok.Data;

/**
 * A summary of factors that can affect the next payment cycle.
 */
@Data
@Builder
public class NextPaymentCycleSummary {

    public static final NextPaymentCycleSummary NO_CHILDREN = NextPaymentCycleSummary.builder().build();

    private int numberOfChildrenTurningOne;
    private int numberOfChildrenTurningFour;
    private boolean childrenUnderFourPresentAtEndOfCycle;

    public boolean hasChildrenTurningFour() {
        return numberOfChildrenTurningFour > 0;
    }

    public boolean youngestChildTurnsFour() {
        return hasChildrenTurningFour() && !childrenUnderFourPresentAtEndOfCycle;
    }

    public boolean hasMultipleChildrenTurningFour() {
        return numberOfChildrenTurningFour > 1;
    }

    public boolean hasChildrenTurningOne() {
        return numberOfChildrenTurningOne > 0;
    }

    public boolean hasMultipleChildrenTurningOne() {
        return numberOfChildrenTurningOne > 1;
    }

    public int getNumberOfChildrenTurningOneOrFour() {
        return numberOfChildrenTurningOne + numberOfChildrenTurningFour;
    }
}
