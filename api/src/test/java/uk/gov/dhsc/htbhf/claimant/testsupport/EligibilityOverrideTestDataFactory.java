package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

public class EligibilityOverrideTestDataFactory {

    public static EligibilityOverride eligibilityConfirmedWithNoChildrenForFiveYears() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                .build();
    }

    public static EligibilityOverride eligibilityNotConfirmed() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.NOT_CONFIRMED)
                .build();
    }
}