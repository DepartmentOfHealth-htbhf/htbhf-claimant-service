package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDateTime;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aValidClaimant;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;

public class ClaimTestDataFactory {

    public static Claim aValidClaim() {
        return aClaimWithClaimStatus(ClaimStatus.ACTIVE);
    }

    public static Claim aClaimWithTooLongFirstName() {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(ClaimantTestDataFactory.aClaimantWithTooLongFirstName())
                .build();
    }

    public static Claim aClaimWithLastName(String lastName) {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE)
                .claimant(ClaimantTestDataFactory.aClaimantWithLastName(lastName))
                .build();
    }

    public static Claim aClaimWithClaimStatus(ClaimStatus claimStatus) {
        return aValidClaimBuilderWithStatus(claimStatus)
                .build();
    }

    public static Claim aClaimWithVoucherEntitlement(String voucherEntitlement) {
        return aValidClaimBuilder()
                .voucherEntitlement(voucherEntitlement)
                .build();
    }

    public static Claim.ClaimBuilder aValidClaimBuilder() {
        return aValidClaimBuilderWithStatus(ClaimStatus.ACTIVE);
    }

    private static Claim.ClaimBuilder aValidClaimBuilderWithStatus(ClaimStatus claimStatus) {
        return Claim.builder()
                .claimant(aValidClaimant())
                .eligibilityStatus(EligibilityStatus.ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .claimStatusTimestamp(LocalDateTime.now())
                .eligibilityStatusTimestamp(LocalDateTime.now())
                .claimStatus(claimStatus);
    }

}
