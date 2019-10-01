package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import static uk.gov.dhsc.htbhf.claimant.creator.entities.claimant.EligibilityStatus.ELIGIBLE;

/**
 * Status of a payment cycle.
 */
public enum PaymentCycleStatus {
    NEW,
    INELIGIBLE,
    READY_FOR_PAYMENT,
    FULL_PAYMENT_MADE,
    PARTIAL_PAYMENT_MADE,
    BALANCE_TOO_HIGH_FOR_PAYMENT;

    /**
     * Get the {@link PaymentCycleStatus} for the given {@link EligibilityStatus}.
     *
     * @param eligibilityStatus The eligibility status
     * @return The payment cycle status
     */
    public static PaymentCycleStatus getStatusForEligibilityDecision(EligibilityStatus eligibilityStatus) {
        return eligibilityStatus == ELIGIBLE ? PaymentCycleStatus.READY_FOR_PAYMENT : PaymentCycleStatus.INELIGIBLE;
    }
}
