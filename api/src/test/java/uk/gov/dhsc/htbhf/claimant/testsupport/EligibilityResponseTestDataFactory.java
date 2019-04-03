package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;

import static uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus.ELIGIBLE;

public class EligibilityResponseTestDataFactory {

    private static final String DWP_HOUSEHOLD_IDENTIFIER = "dwpHousehold1";
    private static final String HMRC_HOUSEHOLD_IDENTIFIER = "hmrcHousehold1";

    public static EligibilityResponse anEligibilityResponse() {
        return aValidEligibilityResponseBuilder().build();
    }

    public static EligibilityResponse anEligibilityResponseWithStatus(EligibilityStatus eligibilityStatus) {
        return EligibilityResponse.builder()
                .eligibilityStatus(eligibilityStatus)
                .dwpHouseholdIdentifier("dwpHousehold1")
                .hmrcHouseholdIdentifier("hmrcHousehold1")
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
