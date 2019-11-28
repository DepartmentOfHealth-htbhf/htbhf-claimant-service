package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimDTOV3;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimantDTOV3;

import java.time.LocalDate;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOV3TestDataFactory.aClaimantDTOWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOV3TestDataFactory.aValidClaimantDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOV3TestDataFactory.aValidClaimantDTOWithNoNullFields;

public final class ClaimDTOV3TestDataFactory {

    private static final String WEB_UI_VERSION = "1.0.0";
    public static final Map<String, Object> DEVICE_FINGERPRINT = Map.of(
            "user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36",
            "x-forwarded-for", "52.215.192.132",
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "accept-encoding", "gzip, deflate, br",
            "accept-language", "en-GB,en-US;q=0.9,en;q=0.8");


    public static ClaimDTOV3 aValidClaimDTO() {
        return aClaimDTOBuilder()
                .build();
    }

    public static ClaimDTOV3 aValidClaimDTOWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithExpectedDeliveryDate(expectedDeliveryDate))
                .build();
    }

    public static ClaimDTOV3 aValidClaimDTOWithNoNullFields() {
        return aClaimDTOBuilder()
                .claimant(aValidClaimantDTOWithNoNullFields())
                .build();
    }

    public static ClaimDTOV3 aClaimDTOWithClaimant(ClaimantDTOV3 claimant) {
        return aClaimDTOBuilder()
                .claimant(claimant)
                .build();
    }

    public static ClaimDTOV3 aClaimDTOWithDeviceFingerprint(Map<String, Object> fingerprint) {
        return aClaimDTOBuilder()
                .deviceFingerprint(fingerprint)
                .build();
    }

    public static ClaimDTOV3 aClaimDTOWithWebUIVersion(String version) {
        return aClaimDTOBuilder()
                .webUIVersion(version)
                .build();
    }

    private static ClaimDTOV3.ClaimDTOV3Builder aClaimDTOBuilder() {
        return ClaimDTOV3.builder()
                .claimant(aValidClaimantDTO())
                .deviceFingerprint(DEVICE_FINGERPRINT)
                .webUIVersion(WEB_UI_VERSION);
    }
}
