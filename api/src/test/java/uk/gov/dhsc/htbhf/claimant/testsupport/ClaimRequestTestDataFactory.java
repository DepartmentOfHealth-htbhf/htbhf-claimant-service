package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest;
import uk.gov.dhsc.htbhf.claimant.service.ClaimRequest.ClaimRequestBuilder;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimDTOTestDataFactory.DEVICE_FINGERPRINT;

public class ClaimRequestTestDataFactory {

    public static final String WEB_UI_VERSION = "1.1.1";

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
