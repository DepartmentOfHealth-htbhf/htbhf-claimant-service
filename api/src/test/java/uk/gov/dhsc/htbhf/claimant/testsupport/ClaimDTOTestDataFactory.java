package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;

import static java.time.LocalDate.now;

public final class ClaimDTOTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";
    private static final LocalDate VALID_DOB = LocalDate.parse("1985-12-31");

    /**
     * Builds a valid {@link ClaimDTO}.
     * @return A valid {@link ClaimDTO}
     */
    public static ClaimDTO aValidClaimDTO() {
        return ClaimDTO.builder()
                .claimant(aValidClaimantBuilder().build())
                .build();
    }

    /**
     * Builds a valid {@link ClaimDTO}.
     * @return A valid {@link ClaimDTO}
     */
    public static ClaimDTO aValidClaimDTOWithNoNullFields() {
        return ClaimDTO.builder()
                .claimant(aValidClaimantBuilder()
                        .expectedDeliveryDate(now().plusMonths(4))
                        .build())
                .build();
    }

    /**
     * Builds a valid {@link ClaimDTO} with the given date of birth.
     * @return A valid {@link ClaimDTO} with the given date of birth
     */
    public static ClaimDTO aClaimDTOWithDateOfBirth(LocalDate dateOfBirth) {
        return ClaimDTO.builder()
                .claimant(aValidClaimantBuilder().dateOfBirth(dateOfBirth).build())
                .build();
    }

    private static ClaimantDTO.ClaimantDTOBuilder aValidClaimantBuilder() {
        return ClaimantDTO.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO)
                .dateOfBirth(VALID_DOB);
    }

}
