package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.dwp.model.v2.IdentityAndEligibilityResponse;
import uk.gov.dhsc.htbhf.dwp.model.v2.VerificationOutcome;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.dwp.testhelper.v2.IdentityAndEligibilityResponseTestDataFactory.anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches;

/**
 * Extension of IdentityAndEligibilityResponseTestDataFactory so we can add common values for the claimant-service.
 */
public class IdAndEligibilityResponseTestDataFactory {

    public static IdentityAndEligibilityResponse anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier() {
        return addHouseholdIdentifier(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(), DWP_HOUSEHOLD_IDENTIFIER);
    }

    public static IdentityAndEligibilityResponse anAllMatchedEligibilityConfirmedUCResponseWithHouseholdIdentifier(List<LocalDate> dateOfBirthOfChildren,
                                                                                                                   String householdIdentifier) {
        return addHouseholdIdentifier(anIdentityMatchedEligibilityConfirmedUCResponseWithAllMatches(
                VerificationOutcome.NOT_SUPPLIED, dateOfBirthOfChildren),
                householdIdentifier);
    }

    public static IdentityAndEligibilityResponse addHouseholdIdentifier(IdentityAndEligibilityResponse originalResponse, String householdIdentifier) {
        return originalResponse.toBuilder()
                .householdIdentifier(householdIdentifier)
                .build();
    }

}
