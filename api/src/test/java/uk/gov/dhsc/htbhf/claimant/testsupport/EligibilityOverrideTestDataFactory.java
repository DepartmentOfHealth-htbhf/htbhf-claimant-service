package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;

public class EligibilityOverrideTestDataFactory {

    public static EligibilityOverride aConfirmedEligibilityOverride() {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .build();
    }

    public static EligibilityOverride aConfirmedEligibilityWithUntilDate(LocalDate overrideUntil) {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(overrideUntil)
                .build();
    }

    public static EligibilityOverride aNotConfirmedEligibilityOverride() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.NOT_CONFIRMED)
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .build();
    }

    private static EligibilityOverride.EligibilityOverrideBuilder aConfirmedEligibilityOverrideBuilder() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED);
    }
}