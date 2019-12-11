package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static uk.gov.dhsc.htbhf.TestConstants.HOMER_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_FORENAME;
import static uk.gov.dhsc.htbhf.TestConstants.HOMER_NINO;
import static uk.gov.dhsc.htbhf.TestConstants.SIMPSON_SURNAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;

public class PersonDTOTestDataFactory {

    public static PersonDTO aValidPerson() {
        return buildDefaultPerson().build();
    }

    private static PersonDTO.PersonDTOBuilder buildDefaultPerson() {
        return PersonDTO.builder()
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .nino(HOMER_NINO)
                .address(aValidAddressDTO())
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME);
    }
}
