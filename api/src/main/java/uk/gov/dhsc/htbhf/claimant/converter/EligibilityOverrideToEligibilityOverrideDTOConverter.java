package uk.gov.dhsc.htbhf.claimant.converter;

import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;

/**
 * Converts a {@link EligibilityOverride} into a {@link EligibilityOverrideDTO}.
 */
@Component
public class EligibilityOverrideToEligibilityOverrideDTOConverter {

    public EligibilityOverrideDTO convert(EligibilityOverride eligibilityOverride) {
        if (eligibilityOverride == null) {
            return null;
        }

        return EligibilityOverrideDTO.builder()
                .overrideUntil(eligibilityOverride.getOverrideUntil())
                .eligibilityOutcome(eligibilityOverride.getEligibilityOutcome())
                .childrenDob(eligibilityOverride.getChildrenDob())
                .build();
    }
}