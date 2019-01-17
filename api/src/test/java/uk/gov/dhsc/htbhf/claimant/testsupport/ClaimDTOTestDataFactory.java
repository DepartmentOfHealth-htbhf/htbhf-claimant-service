package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

@SuppressWarnings("PMD.UseUtilityClass")
public final class ClaimDTOTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";
    private static final String LONG_NAME = "This name is way too long"
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long" //100
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long" //200
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long" //300
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long" //400
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long"
            + "This name is way too long" //500
            + "This name is way too long";

    public static ClaimDTO aValidClaimDTO() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_NINO))
                .build();
    }

    public static ClaimDTO aClaimDTOWithLastNameTooLong() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, LONG_NAME, VALID_NINO))
                .build();
    }

    public static ClaimDTO aClaimDTOWithFirstNameTooLong() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(LONG_NAME, VALID_LAST_NAME, VALID_NINO))
                .build();
    }

    public static ClaimDTO aClaimDTOWithNoLastName() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, null, VALID_NINO))
                .build();
    }

    public static ClaimDTO aClaimDTOWithEmptyLastName() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, "", VALID_NINO))
                .build();
    }

    public static ClaimDTO aClaimDTOWithoutNino() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, VALID_LAST_NAME, null))
                .build();
    }

    public static ClaimDTO aClaimDTOWithInvalidNino() {
        return ClaimDTO.builder()
                .claimant(buildClaimantDTO(VALID_FIRST_NAME, VALID_LAST_NAME, "YYTHQ1239"))
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
