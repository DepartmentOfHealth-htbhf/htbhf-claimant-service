package uk.gov.dhsc.htbhf.claimant.service.v2;

import uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

/**
 * Utility for converting from {@link uk.gov.dhsc.htbhf.dwp.model.v2.EligibilityOutcome} values
 * to {@link uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus} values.
 */
public class EligibilityOutcomeToEligibilityStatusConverter {

    public static EligibilityStatus fromEligibilityOutcome(EligibilityOutcome eligibilityOutcome) {
        if (EligibilityOutcome.CONFIRMED == eligibilityOutcome) {
            return EligibilityStatus.ELIGIBLE;
        } else if (EligibilityOutcome.NOT_CONFIRMED == eligibilityOutcome) {
            return EligibilityStatus.INELIGIBLE;
        } else {
            return EligibilityStatus.NO_MATCH;
        }
    }

}
