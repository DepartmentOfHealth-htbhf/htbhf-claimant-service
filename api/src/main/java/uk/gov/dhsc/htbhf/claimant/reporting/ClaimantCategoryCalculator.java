package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.*;

@Component
@RequiredArgsConstructor
public class ClaimantCategoryCalculator {

    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    /**
     * Determines the {@link uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory} for a given claimant and their children's date of birth.
     *
     * @param claimant               the claimant to determine the category of
     * @param datesOfBirthOfChildren the dates of birth of the claimant's children
     * @param atDate                 the date to use when checking a claimant/child's age or pregnancy
     * @return claimant's determined category
     */
    public ClaimantCategory determineClaimantCategory(Claimant claimant, List<LocalDate> datesOfBirthOfChildren, LocalDate atDate) {
        int claimantAgeInYears = getClaimantAgeInYears(claimant, atDate);
        if (isClaimantPregnant(claimant, atDate)) {
            if (claimantAgeInYears < 16) {
                return PREGNANT_AND_UNDER_16;
            }
            if (claimantAgeInYears < 18) {
                return PREGNANT_AND_UNDER_18;
            }
            if (hasChildren(datesOfBirthOfChildren)) {
                return PREGNANT_WITH_CHILDREN;
            }
            return PREGNANT_WITH_NO_CHILDREN;
        }
        if (hasChildren(datesOfBirthOfChildren)) {
            return NOT_PREGNANT_WITH_CHILDREN;
        }
        // this could happen once a claimant has expired
        return NOT_PREGNANT_WITH_NO_CHILDREN;
    }

    private boolean hasChildren(List<LocalDate> datesOfBirthOfChildren) {
        return datesOfBirthOfChildren != null && !datesOfBirthOfChildren.isEmpty();
    }

    private int getClaimantAgeInYears(Claimant claimant, LocalDate ageAtDate) {
        return Period.between(claimant.getDateOfBirth(), ageAtDate).getYears();
    }

    private boolean isClaimantPregnant(Claimant claimant, LocalDate atDate) {
        return pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate(), atDate);
    }
}
