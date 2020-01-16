package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.EligibilityOverride;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest.ClaimRequestBuilder;

import static uk.gov.dhsc.htbhf.TestConstants.MAGGIE_AND_LISA_DOBS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithChildrenDob;
import static uk.gov.dhsc.htbhf.claimant.testsupport.NewClaimDTOTestDataFactory.DEVICE_FINGERPRINT;


public class ClaimRequestTestDataFactory {

    public static final String WEB_UI_VERSION = "1.1.1";

    public static ClaimRequest aValidClaimRequest() {
        return aClaimRequestBuilderForClaimant(aClaimantWithChildrenDob(MAGGIE_AND_LISA_DOBS)).build();
    }

    public static ClaimRequest aClaimRequestWithEligibilityOverrideOutcome(Claimant claimant, EligibilityOverride eligibilityOverride) {
        return aClaimRequestBuilderForClaimant(claimant).eligibilityOverride(eligibilityOverride).build();
    }

    public static ClaimRequest aClaimRequestForClaimant(Claimant claimant) {
        return aClaimRequestBuilderForClaimant(claimant).build();
    }

    public static ClaimRequestBuilder aClaimRequestBuilderForClaimant(Claimant claimant) {
        return ClaimRequest.builder()
                .claimant(claimant)
                .deviceFingerprint(DEVICE_FINGERPRINT)
                .webUIVersion(WEB_UI_VERSION);
    }
}
