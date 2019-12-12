package uk.gov.dhsc.htbhf.claimant.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.dhsc.htbhf.claimant.entitlement.VoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.containsAny;

@Data
@Builder
public class ClaimResult {

    private Claim claim;
    private Optional<VoucherEntitlement> voucherEntitlement;
    private VerificationResult verificationResult;

    public static ClaimResult withNoEntitlement(Claim claim) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .build();
    }

    public static ClaimResult withNoEntitlement(Claim claim, CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.empty())
                .verificationResult(buildVerificationResult(claim, identityAndEligibilityResponse))
                .build();
    }

    public static ClaimResult withEntitlement(Claim claim, VoucherEntitlement voucherEntitlement,
                                              CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return ClaimResult.builder()
                .claim(claim)
                .voucherEntitlement(Optional.of(voucherEntitlement))
                .verificationResult(buildVerificationResult(claim, identityAndEligibilityResponse))
                .build();
    }

    private static VerificationResult buildVerificationResult(Claim claim, CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
        return VerificationResult.builder()
                .identityOutcome(identityAndEligibilityResponse.getIdentityStatus())
                .eligibilityOutcome(identityAndEligibilityResponse.getEligibilityStatus())
                .addressLine1Match(identityAndEligibilityResponse.getAddressLine1Match())
                .deathVerificationFlag(identityAndEligibilityResponse.getDeathVerificationFlag())
                .emailAddressMatch(identityAndEligibilityResponse.getEmailAddressMatch())
                .mobilePhoneMatch(identityAndEligibilityResponse.getMobilePhoneMatch())
                .postcodeMatch(identityAndEligibilityResponse.getPostcodeMatch())
                .pregnantChildDOBMatch(identityAndEligibilityResponse.getPregnantChildDOBMatch())
                .qualifyingBenefits(identityAndEligibilityResponse.getQualifyingBenefits())
                .isPregnantOrAtLeast1ChildMatched(isPregnantOrAtLeastOneChildMatches(claim, identityAndEligibilityResponse.getDobOfChildrenUnder4()))
                .build();
    }

    private static boolean isPregnantOrAtLeastOneChildMatches(Claim claim, List<LocalDate> registeredChildren) {
        Claimant claimant = claim.getClaimant();
        if (claimant.getExpectedDeliveryDate() != null) { // relying on validation in ClaimantDTO to ensure this is a valid value
            return true;
        }
        List<LocalDate> declaredChildren = claimant.getInitiallyDeclaredChildrenDob();
        if (declaredChildren == null || registeredChildren == null) {
            return false;
        }
        return containsAny(declaredChildren, registeredChildren);
    }
}
