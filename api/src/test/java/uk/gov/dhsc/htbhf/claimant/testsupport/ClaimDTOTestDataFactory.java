package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseUtilityClass"})
public final class ClaimDTOTestDataFactory {

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
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").lastName("Smith").build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithLastNameTooLong() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").lastName(LONG_NAME).build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithFirstNameTooLong() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName(LONG_NAME).lastName("Smith").build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithNoLastName() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").lastName(null).build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithEmptyLastName() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").lastName("").build()
        ).build();
    }

}
