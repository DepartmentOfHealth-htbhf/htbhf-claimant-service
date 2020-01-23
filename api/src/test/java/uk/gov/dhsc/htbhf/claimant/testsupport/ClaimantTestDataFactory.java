package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressTestDataFactory.aValidAddress;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;

public final class ClaimantTestDataFactory {

    // Create a string 501 characters long
    public static final String LONG_NAME = CharBuffer.allocate(501).toString().replace('\0', 'A');

    public static Claimant aValidClaimant() {
        return aValidClaimantBuilder().build();
    }

    public static Claimant aValidClaimantWithNino(String nino) {
        return aValidClaimantBuilder().nino(nino).build();
    }

    public static Claimant aClaimantWithLastName(String lastName) {
        return aValidClaimantBuilder().lastName(lastName).build();
    }

    public static Claimant aClaimantWithChildrenDob(LocalDate... dateOfBirth) {
        return aValidClaimantBuilder().initiallyDeclaredChildrenDob(Arrays.asList(dateOfBirth)).build();
    }

    public static Claimant aClaimantWithChildrenDob(List<LocalDate> childrenDatesOfBirth) {
        return aValidClaimantBuilder().initiallyDeclaredChildrenDob(childrenDatesOfBirth).build();
    }

    public static Claimant aClaimantWithExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        return aValidClaimantBuilder().expectedDeliveryDate(expectedDeliveryDate).build();
    }

    public static Claimant aClaimantWithExpectedDeliveryDateAndChildrenDob(LocalDate expectedDeliveryDate, List<LocalDate> childrenDatesOfBirth) {
        return aValidClaimantBuilder()
                .expectedDeliveryDate(expectedDeliveryDate)
                .initiallyDeclaredChildrenDob(childrenDatesOfBirth)
                .build();
    }

    public static Claimant aClaimantWithCardDeliveryAddress(Address address) {
        return aValidClaimantBuilder().address(address).build();
    }

    public static Claimant aClaimantWithFirstName(String firstName) {
        return aValidClaimantBuilder().firstName(firstName).build();
    }

    public static Claimant aClaimantWithNino(String nino) {
        return aValidClaimantBuilder().nino(nino).build();
    }

    public static Claimant aClaimantWithDateOfBirth(LocalDate dateOfBirth) {
        return aValidClaimantBuilder().dateOfBirth(dateOfBirth).build();
    }

    public static Claimant aClaimantWithFirstNameAndLastName(String firstName, String lastName) {
        return aValidClaimantBuilder().firstName(firstName).lastName(lastName).build();
    }

    public static Claimant aClaimantWithTooLongLastName() {
        return aClaimantWithLastName(LONG_NAME);
    }

    public static Claimant aClaimantWithTooLongFirstName() {
        return aClaimantWithFirstName(LONG_NAME);
    }

    public static Claimant aClaimantWithPhoneNumber(String phoneNumber) {
        return aValidClaimantBuilder()
                .phoneNumber(phoneNumber)
                .build();
    }

    public static Claimant aClaimantWithEmailAddress(String emailAddress) {
        return aValidClaimantBuilder()
                .emailAddress(emailAddress)
                .build();
    }

    public static Claimant.ClaimantBuilder aValidClaimantBuilder() {
        return Claimant.builder()
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME)
                .nino(HOMER_NINO)
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .phoneNumber(HOMER_MOBILE)
                .emailAddress(HOMER_EMAIL)
                .address(aValidAddress())
                .expectedDeliveryDate(EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
    }

    public static Claimant.ClaimantBuilder aValidClaimantInSameHouseholdBuilder() {
        return aValidClaimantBuilder()
                .firstName("Jane")
                .lastName("Smith")
                .nino("BE654321B")
                .dateOfBirth(LocalDate.parse("1990-11-20"))
                .phoneNumber(HOMER_MOBILE)
                .emailAddress(HOMER_EMAIL);
    }
}
