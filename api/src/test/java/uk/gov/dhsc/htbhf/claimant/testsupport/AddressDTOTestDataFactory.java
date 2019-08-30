package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;

public class AddressDTOTestDataFactory {

    private static final String ADDRESS_LINE_1 = "Flat b";
    private static final String ADDRESS_LINE_2 = "123 Fake street";
    private static final String TOWN_OR_CITY = "Springfield";
    private static final String COUNTY = "Devon";
    private static final String POSTCODE = "AA1 1AA";

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
                .addressLine1(ADDRESS_LINE_1)
                .addressLine2(ADDRESS_LINE_2)
                .townOrCity(TOWN_OR_CITY)
                .county(COUNTY)
                .postcode(POSTCODE);
    }
}
