package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;

import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.*;

public class AddressDTOTestDataFactory {

    public static AddressDTO aValidAddressDTO() {
        return aValidAddressDTOBuilder()
                .build();
    }

    public static AddressDTO anAddressDTOWithLine1(String addressLine1) {
        return aValidAddressDTOBuilder()
                .addressLine1(addressLine1)
                .build();
    }

    public static AddressDTO anAddressDTOWithLine2(String addressLine2) {
        return aValidAddressDTOBuilder()
                .addressLine2(addressLine2)
                .build();
    }

    public static AddressDTO anAddressDTOWithTownOrCity(String townOrCity) {
        return aValidAddressDTOBuilder()
                .townOrCity(townOrCity)
                .build();
    }

    public static AddressDTO anAddressDTOWithCounty(String county) {
        return aValidAddressDTOBuilder()
                .county(county)
                .build();
    }

    public static AddressDTO anAddressDTOWithPostcode(String postcode) {
        return aValidAddressDTOBuilder()
                .postcode(postcode)
                .build();
    }

    public static AddressDTO.AddressDTOBuilder aValidAddressDTOBuilder() {
        return AddressDTO.builder()
                .addressLine1(SIMPSONS_ADDRESS_LINE_1)
                .addressLine2(SIMPSONS_ADDRESS_LINE_2)
                .townOrCity(SIMPSONS_TOWN)
                .county(SIMPSONS_COUNTY)
                .postcode(SIMPSONS_POSTCODE);
    }
}
