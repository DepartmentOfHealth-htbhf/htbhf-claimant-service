package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_FORENAME;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_NINO_V1;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.SIMPSON_SURNAME;

public class PersonDTOTestDataFactory {

    public static PersonDTO aValidPerson() {
        return buildDefaultPerson().build();
    }

    private static PersonDTO.PersonDTOBuilder buildDefaultPerson() {
        return PersonDTO.builder()
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .nino(HOMER_NINO_V1)
                .address(aValidAddressDTO())
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME);
    }
}
