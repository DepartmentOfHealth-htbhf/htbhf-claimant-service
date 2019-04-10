package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimResponse;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

public class ClaimResponseTestDataFactory {

    public static ClaimResponse aClaimResponseWithClaimStatus(ClaimStatus claimStatus) {
        return aValidClaimResponseBuilder().claimStatus(claimStatus).build();
    }

    private static ClaimResponse.ClaimResponseBuilder aValidClaimResponseBuilder() {
        return ClaimResponse.builder()
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .claimStatus(ClaimStatus.NEW);
    }
}
