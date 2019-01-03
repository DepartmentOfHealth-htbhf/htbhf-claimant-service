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
                ClaimantDTO.builder().firstName("James").secondName("Smith").build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithSecondNameTooLong() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").secondName(LONG_NAME).build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithFirstNameTooLong() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName(LONG_NAME).secondName("Smith").build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithNoSecondName() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").secondName(null).build()
        ).build();
    }

    public static ClaimDTO aClaimDTOWithEmptySecondName() {
        return ClaimDTO.builder().claimant(
                ClaimantDTO.builder().firstName("James").secondName("").build()
        ).build();
    }

}
