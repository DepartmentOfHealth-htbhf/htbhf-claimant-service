package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;

import static java.time.LocalDate.now;

public final class ClaimDTOTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";
    private static final LocalDate VALID_DOB = LocalDate.parse("1985-12-31");
    public static final String VALID_ADDRESS_LINE_1 = "Flat b";
    public static final String VALID_ADDRESS_LINE_2 = "123 Fake street";
    public static final String VALID_TOWN_OR_CITY = "Springfield";
    public static final String VALID_POSTCODE = "AA1 1AA";

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
                .dateOfBirth(VALID_DOB)
                .cardDeliveryAddress(aValidAddressBuilder().build());
    }

    private static AddressDTO.AddressDTOBuilder aValidAddressBuilder() {
        return AddressDTO.builder()
                .addressLine1(VALID_ADDRESS_LINE_1)
                .addressLine2(VALID_ADDRESS_LINE_2)
                .townOrCity(VALID_TOWN_OR_CITY)
                .postcode(VALID_POSTCODE);
    }

}
