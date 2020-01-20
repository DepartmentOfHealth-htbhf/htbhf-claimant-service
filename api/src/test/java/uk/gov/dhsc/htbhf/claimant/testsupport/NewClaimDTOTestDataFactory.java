package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;
import uk.gov.dhsc.htbhf.claimant.model.EligibilityOverrideDTO;
import uk.gov.dhsc.htbhf.claimant.model.NewClaimDTO;
import uk.gov.dhsc.htbhf.dwp.model.EligibilityOutcome;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.anAddressDTOWithCounty;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.*;

public final class NewClaimDTOTestDataFactory {

    private static final String WEB_UI_VERSION = "1.0.0";
    public static final Map<String, Object> DEVICE_FINGERPRINT = Map.of(
            "user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36",
            "x-forwarded-for", "52.215.192.132",
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "accept-encoding", "gzip, deflate, br",
            "accept-language", "en-GB,en-US;q=0.9,en;q=0.8");


    public static NewClaimDTO aValidClaimDTO() {
        return aClaimDTOBuilder()
                .build();
    }

    public static NewClaimDTO aValidClaimDTOWitChildrenDob(List<LocalDate> childrenDob) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithChildrenDob(childrenDob))
                .build();
    }

    public static NewClaimDTO aValidClaimDTOWithExpectedDeliveryDateAndChildrenDob(LocalDate expectedDeliveryDate, List<LocalDate> childrenDob) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithExpectedDeliveryDateAndChildrenDob(expectedDeliveryDate, childrenDob))
                .build();
    }

    public static NewClaimDTO aValidClaimDTOWithNoNullFields() {
        return aClaimDTOBuilder()
                .claimant(aValidClaimantDTOWithNoNullFields())
                .build();
    }

    public static NewClaimDTO aValidClaimDTOWithEligibilityOverride(LocalDate expectedDeliveryDate,
                                                                    List<LocalDate> childrenDob,
                                                                    EligibilityOutcome eligibilityOutcome,
                                                                    LocalDate overrideUntil
                                                                    ) {
        EligibilityOverrideDTO  eligibilityOverride = EligibilityOverrideDTO.builder()
                .eligibilityOutcome(eligibilityOutcome)
                .overrideUntil(overrideUntil)
                .childrenDob(childrenDob)
                .build();

        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithExpectedDeliveryDateAndChildrenDob(expectedDeliveryDate, childrenDob))
                .eligibilityOverride(eligibilityOverride)
                .build();
    }

    public static NewClaimDTO aValidClaimDTOWithEligibilityOverride(EligibilityOverrideDTO eligibilityOverrideDTO) {
        return aClaimDTOBuilder()
                .eligibilityOverride(eligibilityOverrideDTO)
                .build();
    }

    public static NewClaimDTO aClaimDTOWithClaimant(ClaimantDTO claimant) {
        return aClaimDTOBuilder()
                .claimant(claimant)
                .build();
    }

    public static NewClaimDTO aClaimDTOWithDeviceFingerprint(Map<String, Object> fingerprint) {
        return aClaimDTOBuilder()
                .deviceFingerprint(fingerprint)
                .build();
    }

    public static NewClaimDTO aClaimDTOWithWebUIVersion(String version) {
        return aClaimDTOBuilder()
                .webUIVersion(version)
                .build();
    }

    public static NewClaimDTO aClaimDTOWithCounty(String county) {
        return aClaimDTOBuilder()
                .claimant(aClaimantDTOWithAddress(anAddressDTOWithCounty(county)))
                .build();
    }


    private static NewClaimDTO.NewClaimDTOBuilder aClaimDTOBuilder() {
        return NewClaimDTO.builder()
                .claimant(aValidClaimantDTO())
                .deviceFingerprint(DEVICE_FINGERPRINT)
                .webUIVersion(WEB_UI_VERSION);
    }
}
