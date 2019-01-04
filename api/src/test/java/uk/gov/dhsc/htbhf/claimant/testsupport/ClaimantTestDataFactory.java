package uk.gov.dhsc.htbhf.claimant.testsupport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;

/**
 * Test data factory for {@link Claimant} objects.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClaimantTestDataFactory {

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private static final String LONG_NAME = "This name is way too long"
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
        return Claimant.builder().firstName("Joe").lastName("Blogger").build();
    }

    /**
     * Builds a Claimant with the given last name.
     *
     * @param lastName The last name to use
     * @return The built Claimant
     */
    public static Claimant aClaimantWithLastName(String lastName) {
        return Claimant.builder().firstName("Joe").lastName(lastName).build();
    }

    /**
     * Builds a Claimant with the given first name.
     *
     * @param firstName The first name to use.
     * @return The built Claimant
     */
    public static Claimant aClaimantWithFirstName(String firstName) {
        return Claimant.builder().firstName(firstName).lastName("Smith").build();
    }

    /**
     * Builds an invalid Claimant with a too long second name.
     *
     * @return The built Claimant
     */
    public static Claimant aClaimantWithTooLongLastName() {
        return aClaimantWithLastName(LONG_NAME);
    }

    public static Claimant aClaimantWithTooLongFirstName() {
        return aClaimantWithFirstName(LONG_NAME);
    }
}
