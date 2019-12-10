package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;
import uk.gov.dhsc.htbhf.claimant.model.v3.ClaimantDTOV3;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.singletonList;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOV3TestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

public class ClaimantDTOV3TestDataFactory {

    public static ClaimantDTOV3 aValidClaimantDTO() {
        return aValidClaimantBuilder().build();
    }

    public static ClaimantDTOV3 aValidClaimantDTOWithNoNullFields() {
        return aValidClaimantBuilder()
                .childrenDob(singletonList(MAGGIE_DATE_OF_BIRTH))
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithPhoneNumber(String phoneNumber) {
        return aValidClaimantBuilder()
                .phoneNumber(phoneNumber)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithEmailAddress(String emailAddress) {
        return aValidClaimantBuilder()
                .emailAddress(emailAddress)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithExpectedDeliveryDateAndChildrenDob(LocalDate expectedDeliveryDate, List<LocalDate> childrenDob) {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .childrenDob(childrenDob)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithFirstName(String firstName) {
        return aValidClaimantBuilder()
                .firstName(firstName)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithLastName(String lastName) {
        return aValidClaimantBuilder()
                .lastName(lastName)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithNino(String nino) {
        return aValidClaimantBuilder()
                .nino(nino)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithDateOfBirth(LocalDate dateOfBirth) {
        return aValidClaimantBuilder()
                .dateOfBirth(dateOfBirth)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithAddress(AddressDTOV3 address) {
        return aValidClaimantBuilder()
                .address(address)
                .build();
    }

    public static ClaimantDTOV3 aClaimantDTOWithChildrenDob(List<LocalDate> childrenDob) {
        return aValidClaimantBuilder()
                .childrenDob(childrenDob)
                .build();
    }

    private static ClaimantDTOV3.ClaimantDTOV3Builder aValidClaimantBuilder() {
        return ClaimantDTOV3.builder()
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME)
                .nino(HOMER_NINO_V1)
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .address(aValidAddressDTO())
                .expectedDeliveryDate(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS)
                .phoneNumber(HOMER_MOBILE)
                .emailAddress(HOMER_EMAIL);
    }

}
