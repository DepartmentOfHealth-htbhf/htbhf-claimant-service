package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

import java.nio.CharBuffer;
import java.time.LocalDate;

/**
 * Test data factory for {@link Claimant} objects.
 */
public final class ClaimantTestDataFactory {

    // Create a string 501 characters long
    public static final String LONG_NAME = CharBuffer.allocate(501).toString().replace('\0', 'A');

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "Joe";
    private static final String VALID_LAST_NAME = "Blogger";
    private static final LocalDate VALID_DOB = LocalDate.parse("1987-12-30");
    private static final Address VALID_ADDRESS = AddressTestDataFactory.aValidAddress();


    /**
     * Builds a valid Claimant.
     *
     * @return The built Claimant
     */
    public static Claimant aValidClaimant() {
        return aValidClaimantBuilder().build();
    }

    /**
     * Builds a Claimant with the given last name.
     *
     * @param lastName The last name to use
     * @return The built Claimant
     */
    public static Claimant aClaimantWithLastName(String lastName) {
        return aValidClaimantBuilder().lastName(lastName).build();
    }

    /**
     * Builds a Claimant with the given first name.
     *
     * @param firstName The first name to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithFirstName(String firstName) {
        return aValidClaimantBuilder().firstName(firstName).build();
    }

    /**
     * Builds a Claimant with the given national insurance number.
     *
     * @param nino The national insurance number to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithNino(String nino) {
        return aValidClaimantBuilder().nino(nino).build();
    }

    /**
     * Builds a Claimant with the given date of birth.
     *
     * @param dateOfBirth The date of birth to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithDateOfBirth(LocalDate dateOfBirth) {
        return aValidClaimantBuilder().dateOfBirth(dateOfBirth).build();
    }

    /**
     * Builds a Claimant with the given first name and last name.
     *
     * @param firstName The first name to use.
     * @param lastName The last name to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithFirstNameAndLastName(String firstName, String lastName) {
        return aValidClaimantBuilder().firstName(firstName).lastName(lastName).build();
    }

    /**
     * Builds an invalid Claimant with a too long second name.
     *
     * @return The built Claimant
     */
    public static Claimant aClaimantWithTooLongLastName() {
        return aClaimantWithLastName(LONG_NAME);
    }

    /**
     * Builds an invalid Claimant with a too long first name.
     *
     * @return The built Claimant
     */
    public static Claimant aClaimantWithTooLongFirstName() {
        return aClaimantWithFirstName(LONG_NAME);
    }

    public static Claimant.ClaimantBuilder aValidClaimantBuilder() {
        return Claimant.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO)
                .dateOfBirth(VALID_DOB)
                .cardDeliveryAddress(VALID_ADDRESS);
    }
}
