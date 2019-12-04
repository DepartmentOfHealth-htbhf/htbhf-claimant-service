package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.v3.AddressDTOV3;

import static uk.gov.dhsc.htbhf.TestConstants.*;

public class AddressDTOV3TestDataFactory {

    public static AddressDTOV3 aValidAddressDTO() {
        return aValidAddressDTOBuilder()
                .build();
    }

    public static AddressDTOV3 anAddressDTOWithLine1(String addressLine1) {
        return aValidAddressDTOBuilder()
                .addressLine1(addressLine1)
                .build();
    }

    public static AddressDTOV3 anAddressDTOWithLine2(String addressLine2) {
        return aValidAddressDTOBuilder()
                .addressLine2(addressLine2)
                .build();
    }

    public static AddressDTOV3 anAddressDTOWithTownOrCity(String townOrCity) {
        return aValidAddressDTOBuilder()
                .townOrCity(townOrCity)
                .build();
    }

    public static AddressDTOV3 anAddressDTOWithCounty(String county) {
        return aValidAddressDTOBuilder()
                .county(county)
                .build();
    }

    public static AddressDTOV3 anAddressDTOWithPostcode(String postcode) {
        return aValidAddressDTOBuilder()
                .postcode(postcode)
                .build();
    }

    private static AddressDTOV3.AddressDTOV3Builder aValidAddressDTOBuilder() {
        return AddressDTOV3.builder()
                .addressLine1(SIMPSONS_ADDRESS_LINE_1)
                .addressLine2(SIMPSONS_ADDRESS_LINE_2)
                .townOrCity(SIMPSONS_TOWN)
                .county(SIMPSONS_COUNTY)
                .postcode(SIMPSONS_POSTCODE);
    }
}
