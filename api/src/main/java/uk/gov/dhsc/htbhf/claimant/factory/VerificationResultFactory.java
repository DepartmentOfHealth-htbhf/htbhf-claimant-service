package uk.gov.dhsc.htbhf.claimant.factory;

import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.VerificationResult;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;

import java.time.LocalDate;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.containsAny;

public class VerificationResultFactory {

    /**
     * Creates a {@link VerificationResult} from a given {@link Claimant} and {@link CombinedIdentityAndEligibilityResponse}.
     * @param claimant claimant details
     * @param identityAndEligibilityResponse response from eligibility serivce
     * @return a {@link VerificationResult}
     */
    public static VerificationResult buildVerificationResult(Claimant claimant, CombinedIdentityAndEligibilityResponse identityAndEligibilityResponse) {
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
                .isPregnantOrAtLeast1ChildMatched(isPregnantOrAtLeastOneChildMatches(claimant, identityAndEligibilityResponse.getDobOfChildrenUnder4()))
                .build();
    }

    private static boolean isPregnantOrAtLeastOneChildMatches(Claimant claimant, List<LocalDate> registeredChildren) {
        if (claimant.getExpectedDeliveryDate() != null) {
            return true;
        }

        List<LocalDate> declaredChildren = claimant.getInitiallyDeclaredChildrenDob();
        if (declaredChildren == null || registeredChildren == null) {
            return false;
        }

        return containsAny(declaredChildren, registeredChildren);
    }
}
