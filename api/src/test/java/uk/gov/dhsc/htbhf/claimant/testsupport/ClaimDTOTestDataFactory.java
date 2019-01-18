package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

@SuppressWarnings("PMD.UseUtilityClass")
public final class ClaimDTOTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";

    public static ClaimDTO aValidClaimDTO() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_NINO))
                .build();
    }

    private static ClaimantDTO buildClaimantDTO(String firstName, String lastName, String nino) {
        return ClaimantDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .nino(nino)
                .build();
    }

}
