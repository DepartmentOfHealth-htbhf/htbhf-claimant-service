package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.v2.ClaimantDTO;

import java.time.LocalDate;
import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.*;

public class ClaimantDTOTestDataFactory {

    public static ClaimantDTO aValidClaimantDTO() {
        return aValidClaimantBuilder().build();
    }

    public static ClaimantDTO aValidClaimantDTOWithNoNullFields() {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(now().plusMonths(4))
                .childrenDob(singletonList(MAGGIE_DATE_OF_BIRTH))
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
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME)
                .nino(HOMER_NINO_V1)
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .address(aValidAddressDTO())
                .expectedDeliveryDate(now().plusMonths(1))
                .phoneNumber(HOMER_MOBILE)
                .emailAddress(HOMER_EMAIL);
    }

}
