package uk.gov.dhsc.htbhf.claimant.entity.constraint;

import org.apache.commons.lang3.ObjectUtils;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * An eligibility override can be null but if it is not null then all of the fields must be populated.
 */
public class EligibilityOverrideValidator implements ConstraintValidator<ValidEligibilityOverride, EligibilityOverride> {
    @Override
    public void initialize(ValidEligibilityOverride constraintAnnotation) {
        // No initialization required
    }

    @Override
    public boolean isValid(EligibilityOverride eligibilityOverride, ConstraintValidatorContext context) {
        if (eligibilityOverride == null) {
            return true;
        }

        // none of the fields can be null in a valid EligibilityOverride
        return ObjectUtils.allNotNull(
                eligibilityOverride.getEligibilityOutcome(),
                eligibilityOverride.getOverrideUntil(),
                eligibilityOverride.getChildrenDob()
        );
    }
}
