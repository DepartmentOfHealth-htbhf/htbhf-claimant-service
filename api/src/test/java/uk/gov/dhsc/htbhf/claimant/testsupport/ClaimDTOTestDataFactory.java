package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithCounty;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.*;

public final class ClaimDTOTestDataFactory {

    private static final String WEB_UI_VERSION = "1.0.0";
    public static final Map<String, Object> DEVICE_FINGERPRINT = Map.of(
            "user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36",
            "x-forwarded-for", "52.215.192.132",
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "accept-encoding", "gzip, deflate, br",
            "accept-language", "en-GB,en-US;q=0.9,en;q=0.8");


    public static ClaimDTO aValidClaimDTO() {
        return aClaimDTOBuilder()
                .build();
    }

    public static ClaimDTO aValidClaimDTOWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithExpectedDeliveryDate(expectedDeliveryDate))
                .build();
    }

    public static ClaimDTO aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob(LocalDate expectedDeliveryDate, List<LocalDate> childrenDob) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithExpectedDeliveryDateAndChildrenDob(expectedDeliveryDate, childrenDob))
                .build();
    }

    public static ClaimDTO aValidClaimDTOWithNoNullFields() {
        return aClaimDTOBuilder()
                .claimant(aValidClaimantDTOWithNoNullFields())
                .build();
    }

    public static ClaimDTO aClaimDTOWithClaimant(ClaimantDTO claimant) {
        return aClaimDTOBuilder()
                .claimant(claimant)
                .build();
    }

    public static ClaimDTO aClaimDTOWithDeviceFingerprint(Map<String, Object> fingerprint) {
        return aClaimDTOBuilder()
                .deviceFingerprint(fingerprint)
                .build();
    }

    public static ClaimDTO aClaimDTOWithWebUIVersion(String version) {
        return aClaimDTOBuilder()
                .webUIVersion(version)
                .build();
    }

    public static ClaimDTO aClaimDTOWithCounty(String county) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithAddress(anAddressDTOWithCounty(county)))
                .build();
    }


    private static ClaimDTO.ClaimDTOBuilder aClaimDTOBuilder() {
        return ClaimDTO.builder()
                .claimant(aValidClaimantDTO())
                .deviceFingerprint(DEVICE_FINGERPRINT)
                .webUIVersion(WEB_UI_VERSION);
    }
}
