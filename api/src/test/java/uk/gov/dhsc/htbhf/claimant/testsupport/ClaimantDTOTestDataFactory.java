package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.ClaimantDTO;

import java.time.LocalDate;
import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;

public class ClaimantDTOTestDataFactory {

    public static ClaimantDTO aValidClaimantDTO() {
        return aValidClaimantBuilder().build();
    }

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

    public static ClaimantDTO aClaimantDTOWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithFirstName(String firstName) {
        return aValidClaimantBuilder()
                .firstName(firstName)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithLastName(String lastName) {
        return aValidClaimantBuilder()
                .lastName(lastName)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithNino(String nino) {
        return aValidClaimantBuilder()
                .nino(nino)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithDateOfBirth(LocalDate dateOfBirth) {
        return aValidClaimantBuilder()
                .dateOfBirth(dateOfBirth)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithAddress(AddressDTO address) {
        return aValidClaimantBuilder()
                .address(address)
                .build();
    }

    public static ClaimantDTO aClaimantDTOWithChildrenDob(List<LocalDate> childrenDob) {
        return aValidClaimantBuilder()
                .childrenDob(childrenDob)
                .build();
    }

    private static ClaimantDTO.ClaimantDTOBuilder aValidClaimantBuilder() {
        return ClaimantDTO.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO)
                .dateOfBirth(VALID_DOB)
                .address(aValidAddressDTO())
                .expectedDeliveryDate(now().plusMonths(1))
                .phoneNumber(VALID_PHONE_NUMBER)
                .emailAddress(VALID_EMAIL_ADDRESS);
    }

}
