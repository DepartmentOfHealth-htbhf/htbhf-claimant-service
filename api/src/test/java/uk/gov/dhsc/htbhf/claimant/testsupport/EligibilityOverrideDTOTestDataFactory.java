package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.OVERRIDE_UNTIL_FIVE_YEARS;

public class EligibilityOverrideDTOTestDataFactory {

    public static EligibilityOverrideDTO aConfirmedEligibilityOverrideDTOWithChildren(List<LocalDate> childrenDob) {
        return EligibilityOverrideDTO.builder()
                .eligibilityOutcome(EligibilityOutcome.CONFIRMED)
                .overrideUntil(OVERRIDE_UNTIL_FIVE_YEARS)
                .childrenDob(childrenDob)
                .build();
    }
}
