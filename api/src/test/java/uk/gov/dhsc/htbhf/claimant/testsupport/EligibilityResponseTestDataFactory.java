package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DWP_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.HMRC_HOUSEHOLD_IDENTIFIER;
import static uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus.ELIGIBLE;

public class EligibilityResponseTestDataFactory {

    public static EligibilityResponse anEligibilityResponse() {
        return aValidEligibilityResponseBuilder().build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER)
                .build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatusOnly(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .build();
    }

    public static EligibilityResponse anEligibilityResponseWithDwpHouseholdIdentifier(String dwpHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().dwpHouseholdIdentifier(dwpHouseholdIdentifier).build();
    }

    public static EligibilityResponse anEligibilityResponseWithHmrcHouseholdIdentifier(String hmrcHouseholdIdentifier) {
        return aValidEligibilityResponseBuilder().hmrcHouseholdIdentifier(hmrcHouseholdIdentifier).build();
    }

    public static EligibilityResponse.EligibilityResponseBuilder aValidEligibilityResponseBuilder() {
        return EligibilityResponse.builder()
                .eligibilityStatus(ELIGIBLE)
                .dwpHouseholdIdentifier(DWP_HOUSEHOLD_IDENTIFIER)
                .hmrcHouseholdIdentifier(HMRC_HOUSEHOLD_IDENTIFIER);
    }

}
