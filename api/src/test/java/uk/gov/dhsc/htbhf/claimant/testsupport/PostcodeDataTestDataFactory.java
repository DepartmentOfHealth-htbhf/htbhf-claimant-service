package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeDataCodes;

public class PostcodeDataTestDataFactory {

    public static PostcodeData aPostcodeDataObjectForPostcode(String postcode) {
        return PostcodeData.builder()
                .postcode(postcode)
                .quality(1)
                .eastings(358705)
                .northings(173153)
                .country("England")
                .nhsHa("South West")
                .longitude(-2.595721)
                .latitude(51.455841)
                .europeanElectoralRegion("South West")
                .primaryCareTrust("Bristol")
                .region("South West")
                .lsoa("Bristol 032B")
                .msoa("Bristol 032")
                .incode("4TB")
                .outcode("BS1")
                .parliamentaryConstituency("Bristol West")
                .adminDistrict("Bristol, City of")
                .parish("Bristol, City of, unparished area")
                .adminCounty(null)
                .adminWard("Central")
                .ced(null)
                .ccg("NHS Bristol, North Somerset and South Gloucestershire")
                .nuts("Bristol, City of")
                .codes(aPostcodesDataCodeObject())
                .build();

    }

    private static PostcodeDataCodes aPostcodesDataCodeObject() {
        return PostcodeDataCodes.builder()
                .adminDistrict("E06000023")
                .adminCounty("E99999999")
                .adminWard("E05010892")
                .parish("E43000019")
                .parliamentaryConstituency("E14000602")
                .ccg("E38000222")
                .ced("E99999999")
                .nuts("UKK11")
                .build();
    }

}
