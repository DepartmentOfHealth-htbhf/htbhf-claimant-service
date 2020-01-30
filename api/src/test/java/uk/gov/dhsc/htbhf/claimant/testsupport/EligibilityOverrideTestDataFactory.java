package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.TestConstants.NO_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;
import static uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome.CONFIRMED;
import static uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome.NOT_CONFIRMED;
import static uk.gov.dhsc.htbhf.dwp.model.QualifyingBenefits.UNDER_18;

public class EligibilityOverrideTestDataFactory {

    public static EligibilityOverride aConfirmedEligibilityOverrideWithNoChildren() {
        return aConfirmedEligibilityOverrideWithNoChildrenOverrideUntilBuilder(OVERRIDE_UNTIL_FIVE_YEARS)
                .build();
    }

    public static EligibilityOverride aConfirmedEligibilityOverrideWithChildren(List<LocalDate> childrenDob) {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(childrenDob)
                .build();
    }

    public static EligibilityOverride aConfirmedEligibilityWithNoChildrenOverriddenUntil(LocalDate overrideUntil) {
        return aConfirmedEligibilityOverrideWithNoChildrenOverrideUntilBuilder(overrideUntil)
                .build();
    }

    private static EligibilityOverride.EligibilityOverrideBuilder aConfirmedEligibilityOverrideWithNoChildrenOverrideUntilBuilder(LocalDate overrideUntil) {
        return aConfirmedEligibilityOverrideBuilder()
                .overrideUntil(overrideUntil)
                .childrenDob(NO_CHILDREN);
    }

    public static EligibilityOverride aConfirmedEligibilityForUnder18Pregnant(LocalDate overrideUntil) {
        return aConfirmedEligibilityOverrideWithNoChildrenOverrideUntilBuilder(overrideUntil)
                .qualifyingBenefits(UNDER_18)
                .build();
    }

    public static EligibilityOverride aNotConfirmedEligibilityOverride() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(NOT_CONFIRMED)
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(NO_CHILDREN)
                .build();
    }

    private static EligibilityOverride.EligibilityOverrideBuilder aConfirmedEligibilityOverrideBuilder() {
        return EligibilityOverride.builder()
                .eligibilityOutcome(CONFIRMED);
    }
}