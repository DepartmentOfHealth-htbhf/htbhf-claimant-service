package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;

public class ClaimTestDataFactory {

    public static Claim aClaimWithClaimStatus(ClaimStatus claimStatus) {
        return Claim.builder()
                .claimant(aValidClaimantBuilder().build())
                .claimStatus(claimStatus)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .claimStatusTimestamp(LocalDateTime.now())
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .build();
    }

}
