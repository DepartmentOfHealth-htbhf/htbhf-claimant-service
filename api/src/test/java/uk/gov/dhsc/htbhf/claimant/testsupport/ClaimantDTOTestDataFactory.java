package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;

public class ClaimantDTOTestDataFactory {

    public static ClaimantDTO aValidClaimantDTOWithNoNullFields() {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(now().plusMonths(4))
                .childrenDob(singletonList(MAGGIE_DOB))
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithPhoneNumber(String phoneNumber) {
        return aValidClaimantBuilder()
                .phoneNumber(phoneNumber)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithEmailAddress(String emailAddress) {
        return aValidClaimantBuilder()
                .emailAddress(emailAddress)
                .build();
    }

    public static ClaimantDTO.ClaimantDTOBuilder aValidClaimantBuilder() {
        return ClaimantDTO.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO)
                .dateOfBirth(VALID_DOB)
                .address(aValidAddressBuilder().build())
                .expectedDeliveryDate(now().plusMonths(1))
                .phoneNumber(VALID_PHONE_NUMBER)
                .emailAddress(VALID_EMAIL_ADDRESS);
    }

    public static AddressDTO.AddressDTOBuilder aValidAddressBuilder() {
        return AddressDTO.builder()
                .addressLine1(VALID_ADDRESS_LINE_1)
                .addressLine2(VALID_ADDRESS_LINE_2)
                .townOrCity(VALID_TOWN_OR_CITY)
                .postcode(VALID_POSTCODE);
    }
}
