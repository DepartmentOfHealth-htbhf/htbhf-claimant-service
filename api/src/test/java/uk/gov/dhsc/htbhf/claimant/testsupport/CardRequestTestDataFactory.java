package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.AddressDTO;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.HOMER_FORENAME;
import static uk.gov.dhsc.htbhf.dwp.testhelper.TestConstants.SIMPSON_SURNAME;

public class CardRequestTestDataFactory {

    private static final String EMAIL = "test@email.com";
    private static final String MOBILE = "07700900000";
    private static final String CLAIM_ID = "6bfdbf4a-fb53-4fb6-ae3a-414a660bf3fc";

    public static CardRequest aValidCardRequest() {
        return CardRequest.builder()
                .address(aValidAddressDTO())
                .claimId(CLAIM_ID)
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME)
                .email(EMAIL)
                .mobile(MOBILE)
                .build();

    }

    public static CardRequest aCardRequest(Claim claim) {
        Claimant claimant = claim.getClaimant();
        Address address = claimant.getAddress();
        return CardRequest.builder()
                .address(AddressDTO.builder()
                        .addressLine1(address.getAddressLine1())
                        .addressLine2(address.getAddressLine2())
                        .townOrCity(address.getTownOrCity())
                        .postcode(address.getPostcode())
                        .county(address.getCounty())
                        .build())
                .claimId(claim.getId().toString())
                .dateOfBirth(claimant.getDateOfBirth())
                .firstName(claimant.getFirstName())
                .lastName(claimant.getLastName())
                .email(claimant.getEmailAddress())
                .mobile(claimant.getPhoneNumber())
                .build();
    }

}
