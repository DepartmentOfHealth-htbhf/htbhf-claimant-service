package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;

import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;

/**
 * Extension of IdentityAndEligibilityResponseTestDataFactory so we can add common values for the claimant-service.
 */
public class ClaimantServiceIdentityAndEligibilityResponseTestDataFactory {

    public static IdentityAndEligibilityResponse anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier() {
        return addHouseholdIdentifier(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches());
    }

    public static IdentityAndEligibilityResponse addHouseholdIdentifier(IdentityAndEligibilityResponse originalResponse) {
        return originalResponse.toBuilder()
                .householdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .build();
    }

}
