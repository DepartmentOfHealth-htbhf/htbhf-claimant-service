package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;

public class EligibilityOverrideTestDataFactory {

    public static EligibilityOverride aConfirmedEligibilityOverride() {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(emptyList())
                .build();
    }

    public static EligibilityOverride aConfirmedEligibilityOverrideWithChildren(List<LocalDate> childrenDob) {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(childrenDob)
                .build();
    }

    public static EligibilityOverride aConfirmedEligibilityWithUntilDate(LocalDate overrideUntil) {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(overrideUntil)
                .childrenDob(emptyList())
                .build();
    }

    public static EligibilityOverride aNotConfirmedEligibilityOverride() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.NOT_CONFIRMED)
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(emptyList())
                .build();
    }

    private static EligibilityOverride.EligibilityOverrideBuilder aConfirmedEligibilityOverrideBuilder() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED);
    }
}