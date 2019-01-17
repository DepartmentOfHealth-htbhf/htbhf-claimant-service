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

    private static Claimant.ClaimantBuilder aValidClaimantBuilder() {
        return Claimant.builder()
                .firstName(VALID_FIRST_NAME)
                .lastName(VALID_LAST_NAME)
                .nino(VALID_NINO);
    }
}
