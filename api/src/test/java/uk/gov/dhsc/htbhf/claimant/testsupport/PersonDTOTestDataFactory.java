package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.eligibility.PersonDTO;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LAST_NAME;

public class PersonDTOTestDataFactory {

    private static final String NINO = "EB123456C";

    public static PersonDTO aValidPerson() {
        return buildDefaultPerson().build();
    }

    private static PersonDTO.PersonDTOBuilder buildDefaultPerson() {
        return PersonDTO.builder()
                .dateOfBirth(DATE_OF_BIRTH)
                .nino(NINO)
                .address(aValidAddressDTO())
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME);
    }
}
