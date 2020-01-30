package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.*;

@Component
@RequiredArgsConstructor
public class ClaimantCategoryCalculator {

    private static final int SIXTEEN = 16;
    private static final int EIGHTEEN = 18;

    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;
    private final ChildDateOfBirthCalculator childDateOfBirthCalculator;

    /**
     * Determines the {@link uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory} for a given claimant and their children's date of birth.
     *
     * @param claimant               the claimant to determine the category of
     * @param datesOfBirthOfChildren the dates of birth of the claimant's children
     * @param atDate                 the date to use when checking a claimant/child's age or pregnancy
     * @param eligibilityOverride    overrides the reason that this applicant qualifies for Healthy Start
     * @return claimant's determined category
     */
    public ClaimantCategory determineClaimantCategory(Claimant claimant,
                                                      List<LocalDate> datesOfBirthOfChildren,
                                                      LocalDate atDate,
                                                      EligibilityOverride eligibilityOverride) {
        if (isClaimantPregnant(claimant, atDate, eligibilityOverride)) {
            int claimantAgeInYears = getClaimantAgeInYears(claimant, atDate);
            if (claimantAgeInYears < SIXTEEN) {
                return PREGNANT_AND_UNDER_16;
            }
            if (claimantAgeInYears < EIGHTEEN) {
                return PREGNANT_AND_UNDER_18;
            }
            if (childDateOfBirthCalculator.hasChildrenUnderFourAtGivenDate(datesOfBirthOfChildren, atDate)) {
                return PREGNANT_WITH_CHILDREN;
            }
            return PREGNANT_WITH_NO_CHILDREN;
        }
        if (childDateOfBirthCalculator.hasChildrenUnderFourAtGivenDate(datesOfBirthOfChildren, atDate)) {
            return NOT_PREGNANT_WITH_CHILDREN;
        }
        // this could happen once a claim has expired
        return NOT_PREGNANT_WITH_NO_CHILDREN;
    }

    private int getClaimantAgeInYears(Claimant claimant, LocalDate atDate) {
        return Period.between(claimant.getDateOfBirth(), atDate).getYears();
    }

    private boolean isClaimantPregnant(Claimant claimant, LocalDate atDate, EligibilityOverride eligibilityOverride) {
        return pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate(), atDate, eligibilityOverride);
    }
}
