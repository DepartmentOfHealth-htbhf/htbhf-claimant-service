package uk.gov.dhsc.htbhf.claimant.reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entitlement.PregnancyEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.NOT_PREGNANT_WITH_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.PREGNANT_AND_UNDER_16;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.PREGNANT_WITH_CHILDREN;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory.PREGNANT_WITH_NO_CHILDREN;

@Component
@RequiredArgsConstructor
public class ClaimantCategoryCalculator {

    private final PregnancyEntitlementCalculator pregnancyEntitlementCalculator;

    /**
     * Determines the {@link uk.gov.dhsc.htbhf.claimant.reporting.payload.ClaimantCategory} for a given claimant and their children's date of birth.
     *
     * @param claimant               the claimant to determine the category of
     * @param datesOfBirthOfChildren the dates of birth of the claimant's children
     * @param timestamp the timestamp to use when checking a claimant/child's age
     * @return claimant's determined category
     */
    public String determineClaimantCategory(Claimant claimant, List<LocalDate> datesOfBirthOfChildren, LocalDateTime timestamp) {
        int claimantAgeInYears = getClaimantAgeInYears(claimant, timestamp);
        if (claimantAgeInYears < 16 && isClaimantPregnant(claimant)) {
            return PREGNANT_AND_UNDER_16.getDescription();
        } else if (isClaimantPregnant(claimant) && hasChildren(datesOfBirthOfChildren)) {
            return PREGNANT_WITH_CHILDREN.getDescription();
        } else if (isClaimantPregnant(claimant) && !hasChildren(datesOfBirthOfChildren)) {
            return PREGNANT_WITH_NO_CHILDREN.getDescription();
        } else if (!isClaimantPregnant(claimant) && hasChildren(datesOfBirthOfChildren)) {
            return NOT_PREGNANT_WITH_CHILDREN.getDescription();
        }

        // TODO DW HTBHF-2010 handle case where claimant doesn't match any category.
        return null;
    }

    private boolean hasChildren(List<LocalDate> datesOfBirthOfChildren) {
        return datesOfBirthOfChildren != null && !datesOfBirthOfChildren.isEmpty();
    }

    private int getClaimantAgeInYears(Claimant claimant, LocalDateTime timestamp) {
        return Period.between(claimant.getDateOfBirth(), timestamp.toLocalDate()).getYears();
    }

    private boolean isClaimantPregnant(Claimant claimant) {
        LocalDate now = LocalDate.now();
        return pregnancyEntitlementCalculator.isEntitledToVoucher(claimant.getExpectedDeliveryDate(), now);
    }
}
