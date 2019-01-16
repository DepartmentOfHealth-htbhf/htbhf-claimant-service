package uk.gov.dhsc.htbhf.claimant.testsupport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

/**
 * Test data factory for {@link Claimant} objects.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClaimantTestDataFactory {

    private static final String VALID_NINO = "QQ123456C";
    private static final String VALID_FIRST_NAME = "Joe";
    private static final String VALID_LAST_NAME = "Blogger";

    public static final String LONG_NAME = "This name is way too long"
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

    /**
     * Builds a valid Claimant.
     *
     * @return The built Claimant
     */
    public static Claimant aValidClaimant() {
        return buildClaimant(VALID_FIRST_NAME, VALID_LAST_NAME, VALID_NINO);
    }

    /**
     * Builds a Claimant with the given last name.
     *
     * @param lastName The last name to use
     * @return The built Claimant
     */
    public static Claimant aClaimantWithLastName(String lastName) {
        return buildClaimant(VALID_FIRST_NAME, lastName, VALID_NINO);
    }

    /**
     * Builds a Claimant with the given first name.
     *
     * @param firstName The first name to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithFirstName(String firstName) {
        return buildClaimant(firstName, VALID_LAST_NAME, VALID_NINO);
    }

    /**
     * Builds a Claimant with the given national insurance number.
     *
     * @param nino The national insurance number to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithNino(String nino) {
        return buildClaimant(VALID_FIRST_NAME, VALID_LAST_NAME, nino);
    }

    /**
     * Builds a Claimant with the given first name and last name.
     *
     * @param firstName The first name to use.
     * @param lastName The last name to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithFirstNameAndLastName(String firstName, String lastName) {
        return buildClaimant(firstName, lastName, VALID_NINO);
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

    private static Claimant buildClaimant(String firstName, String lastName, String nino) {
        return Claimant.builder()
                .firstName(firstName)
                .lastName(lastName)
                .nino(nino)
                .build();
    }
}
