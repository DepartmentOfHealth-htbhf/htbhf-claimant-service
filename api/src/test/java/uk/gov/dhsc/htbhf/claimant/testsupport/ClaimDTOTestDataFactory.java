package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;

@SuppressWarnings("PMD.UseUtilityClass")
public final class ClaimDTOTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";
    private static final LocalDate VALID_DOB = LocalDate.parse("1985-12-31");

    public static ClaimDTO aValidClaimDTO() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_NINO, VALID_DOB))
                .build();
    }

    private static ClaimantDTO buildClaimantDTO(String firstName, String lastName, String nino, LocalDate dateOfBirth) {
        return ClaimantDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .nino(nino)
                .dateOfBirth(dateOfBirth)
                .build();
    }

}
