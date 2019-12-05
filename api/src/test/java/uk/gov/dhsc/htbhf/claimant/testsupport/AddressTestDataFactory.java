package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;

import static uk.gov.dhsc.htbhf.TestConstants.*;

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

    public static Address anAddressWithCounty(String county) {
        return aValidAddressBuilder().county(county).build();
    }

    public static Address anAddressWithPostcode(String postcode) {
        return aValidAddressBuilder().postcode(postcode).build();
    }

    public static Address.AddressBuilder aValidAddressBuilder() {
        return Address.builder()
                .addressLine1(SIMPSONS_ADDRESS_LINE_1)
                .addressLine2(SIMPSONS_ADDRESS_LINE_2)
                .townOrCity(SIMPSONS_TOWN)
                .county(SIMPSONS_COUNTY)
                .postcode(SIMPSONS_POSTCODE);
    }
}
