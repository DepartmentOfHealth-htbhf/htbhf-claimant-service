package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;

public class AddressTestDataFactory {

    public static Address aValidAddress() {
        return aValidAddressBuilder().build();
    }

    public static Address anAddressWithAddressLine1(String addressLine1) {
        return aValidAddressBuilder().addressLine1(addressLine1).build();
    }

    public static Address anAddressWithAddressLine2(String addressLine2) {
        return aValidAddressBuilder().addressLine2(addressLine2).build();
    }

    public static Address anAddressWithTownOrCity(String townOrCity) {
        return aValidAddressBuilder().townOrCity(townOrCity).build();
    }

    public static Address anAddressWithPostcode(String postcode) {
        return aValidAddressBuilder().postcode(postcode).build();
    }

    private static Address.AddressBuilder aValidAddressBuilder() {
        return Address.builder()
                .addressLine1("Flat b")
                .addressLine2("123 Fake street")
                .townOrCity("Springfield")
                .postcode("AA1 1AA");
    }
}
