package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;

public class EligibilityResponseTestDataFactory {

    public static EligibilityResponse anEligibilityResponseWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .dwpHouseholdIdentifier("dwpHousehold1")
                .hmrcHouseholdIdentifier("hmrcHousehold1")
                .build();
    }
}
