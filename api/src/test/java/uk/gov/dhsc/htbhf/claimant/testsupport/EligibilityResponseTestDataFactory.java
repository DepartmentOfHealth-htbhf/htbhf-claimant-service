package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;

import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.ELIGIBLE;

public class EligibilityResponseTestDataFactory {

    public static EligibilityResponse anEligibilityResponse() {
        return EligibilityResponse.builder()
                .eligibilityStatus(ELIGIBLE)
                .build();
    }

}
