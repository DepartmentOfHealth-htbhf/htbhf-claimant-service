package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityStatus;

import java.nio.CharBuffer;
import java.time.LocalDate;

public final class ClaimantTestDataFactory {

    // Create a string 501 characters long
    public static final String LONG_NAME = CharBuffer.allocate(501).toString().replace('\0', 'A');

    private static final String VALID_NINO = "EB123456C";
    private static final String VALID_FIRST_NAME = "James";
    private static final String VALID_LAST_NAME = "Smith";
    private static final LocalDate VALID_DOB = LocalDate.parse("1987-12-30");
    private static final Address VALID_ADDRESS = AddressTestDataFactory.aValidAddress();


    public static Claimant aClaimantWithoutEligibilityStatus() {
        return aValidClaimantBuilder().eligibilityStatus(null).build();
    }

    public static Claimant aValidClaimantWithEligibilityStatus() {
        return aValidClaimantBuilder().build();
    }

    public static Claimant aClaimantWithLastName(String lastName) {
        return aValidClaimantBuilder().lastName(lastName).build();
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

    public static Claimant.ClaimantBuilder aValidClaimantBuilder() {
        return Claimant.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO)
                .dateOfBirth(VALID_DOB)
                .cardDeliveryAddress(VALID_ADDRESS)
                .eligibilityStatus(EligibilityStatus.ELIGIBLE);
    }
}
