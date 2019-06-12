package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aValidClaimantBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantDTOTestDataFactory.aValidClaimantDTOWithNoNullFields;

public final class ClaimDTOTestDataFactory {

    public static final Map<String, Object> DEVICE_FINGERPRINT = Map.of(
            "user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36",
            "x-forwarded-for", "52.215.192.132",
            "accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "accept-encoding", "gzip, deflate, br",
            "accept-language", "en-GB,en-US;q=0.9,en;q=0.8");


    public static ClaimDTO aValidClaimDTO() {
        return aClaimDTOBuilder()
                .claimant(aValidClaimantBuilder().build())
                .build();
    }

    public static ClaimDTO aValidClaimDTOWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aClaimDTOBuilder()
                .claimant(aValidClaimantBuilder()
                        .expectedDeliveryDate(expectedDeliveryDate)
                        .build())
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

    private static ClaimDTO.ClaimDTOBuilder aClaimDTOBuilder() {
        return ClaimDTO.builder()
                .deviceFingerprint(DEVICE_FINGERPRINT);
    }
}
