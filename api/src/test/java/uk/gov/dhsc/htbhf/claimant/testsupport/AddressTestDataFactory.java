package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;

/**
 * Test data factory for {@link uk.gov.dhsc.htbhf.claimant.entity.Address} objects.
 */
public class AddressTestDataFactory {

    /**
     * Builds a valid Address.
     *
     * @return The built Address
     */
    public static Address aValidAddress() {
        return aValidAddressBuilder().build();
    }

    /**
     * Builds an address with the given address line 1.
     *
     * @param addressLine1 The address line 1 to use
     * @return The built Address
     */
    public static Address anAddressWithAddressLine1(String addressLine1) {
        return aValidAddressBuilder().addressLine1(addressLine1).build();
    }

    /**
     * Builds an address with the given address line 2.
     *
     * @param addressLine2 The address line 2 to use
     * @return The built Address
     */
    public static Address anAddressWithAddressLine2(String addressLine2) {
        return aValidAddressBuilder().addressLine2(addressLine2).build();
    }

    /**
     * Builds an address with the given town or city field.
     *
     * @param townOrCity The town or city to use
     * @return The built Address
     */
    public static Address anAddressWithTownOrCity(String townOrCity) {
        return aValidAddressBuilder().townOrCity(townOrCity).build();
    }

    /**
     * Builds an address with the given postcode.
     *
     * @param postcode The postcode to use
     * @return The built Address
     */
    public static Address anAddressWithPostcode(String postcode) {
        return aValidAddressBuilder().postcode(postcode).build();
    }

    private static Address.AddressBuilder aValidAddressBuilder() {
        return Address.builder()
                .addressLine1("Flat 5")
                .addressLine2("123 Fake street")
                .townOrCity("Bristol")
                .postcode("AA11AA");
    }
}
