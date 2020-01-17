package uk.gov.dhsc.htbhf.claimant.entity.constraint;

import org.apache.commons.lang3.ObjectUtils;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EligibilityOverrideValidator implements ConstraintValidator<ValidEligibilityOverride, EligibilityOverride> {
    @Override
    public void initialize(ValidEligibilityOverride constraintAnnotation) {
        //No initialization required
    }

    @Override
    public boolean isValid(EligibilityOverride eligibilityOverride, ConstraintValidatorContext context) {
        if (null == eligibilityOverride) {
            return true;
        }

        return ObjectUtils.allNotNull(
                eligibilityOverride.getEligibilityOutcome(),
                eligibilityOverride.getOverrideUntil()
        );
    }
}
