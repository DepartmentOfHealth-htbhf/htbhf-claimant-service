package uk.gov.dhsc.htbhf.claimant.service.v2;

import org.springframework.util.CollectionUtils;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.*;
import uk.gov.dhsc.htbhf.eligibility.model.CombinedIdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Factory object for building CombinedIdentityAndEligibilityResponse objects for v1 responses from the
 * eligibility service.
 */
public class CombinedIdentityAndEligibilityResponseFactory {

    private static final String NO_HOUSEHOLD_IDENTIFIER_SET = "";

    /**
     * Builds up an {@link CombinedIdentityAndEligibilityResponse} matching the {@link EligibilityResponse} returned from
     * the v1 version of the eligibility client.
     *
     * @param eligibilityResponse The response to use
     * @return The built response.
     */
    public static CombinedIdentityAndEligibilityResponse fromEligibilityResponse(EligibilityResponse eligibilityResponse) {
        EligibilityStatus eligibilityStatus = eligibilityResponse.getEligibilityStatus();
        CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder builder = setupBuilder();
        if (EligibilityStatus.ELIGIBLE == eligibilityStatus) {
            buildEligibleResponse(builder, eligibilityResponse);
        } else if (EligibilityStatus.INELIGIBLE == eligibilityStatus) {
            buildIneligibleResponse(builder);
        } else {
            buildNotMatchedResponse(builder);
        }
        return builder.build();
    }

    private static void buildNotMatchedResponse(CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder builder) {
        builder
                .identityStatus(IdentityOutcome.NOT_MATCHED)
                .eligibilityStatus(EligibilityOutcome.NOT_SET)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET);
    }

    private static void buildIneligibleResponse(CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder builder) {
        builder
                .identityStatus(IdentityOutcome.MATCHED)
                .eligibilityStatus(EligibilityOutcome.NOT_CONFIRMED)
                .qualifyingBenefits(QualifyingBenefits.NOT_SET)
                .mobilePhoneMatch(VerificationOutcome.NOT_SET)
                .emailAddressMatch(VerificationOutcome.NOT_SET)
                .addressLine1Match(VerificationOutcome.NOT_SET)
                .postcodeMatch(VerificationOutcome.NOT_SET)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SET);
    }

    private static void buildEligibleResponse(CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder builder,
                                              EligibilityResponse eligibilityResponse) {
        builder
                .identityStatus(IdentityOutcome.MATCHED)
                .eligibilityStatus(EligibilityOutcome.CONFIRMED)
                .qualifyingBenefits(QualifyingBenefits.UNIVERSAL_CREDIT)
                .mobilePhoneMatch(VerificationOutcome.MATCHED)
                .emailAddressMatch(VerificationOutcome.MATCHED)
                .addressLine1Match(VerificationOutcome.MATCHED)
                .postcodeMatch(VerificationOutcome.MATCHED)
                .pregnantChildDOBMatch(VerificationOutcome.NOT_SUPPLIED)
                .dwpHouseholdIdentifier(eligibilityResponse.getDwpHouseholdIdentifier())
                .hmrcHouseholdIdentifier(eligibilityResponse.getHmrcHouseholdIdentifier())
                .dobOfChildrenUnder4(nullSafeGetChildrenDob(eligibilityResponse));
    }

    private static CombinedIdentityAndEligibilityResponse.CombinedIdentityAndEligibilityResponseBuilder setupBuilder() {
        return CombinedIdentityAndEligibilityResponse.builder()
                .deathVerificationFlag(DeathVerificationFlag.N_A)
                .dwpHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_SET)
                .hmrcHouseholdIdentifier(NO_HOUSEHOLD_IDENTIFIER_SET)
                .dobOfChildrenUnder4(emptyList());
    }

    private static List<LocalDate> nullSafeGetChildrenDob(EligibilityResponse eligibilityResponse) {
        List<LocalDate> dateOfBirthOfChildren = eligibilityResponse.getDateOfBirthOfChildren();
        return CollectionUtils.isEmpty(dateOfBirthOfChildren) ? emptyList() : dateOfBirthOfChildren;
    }
}
