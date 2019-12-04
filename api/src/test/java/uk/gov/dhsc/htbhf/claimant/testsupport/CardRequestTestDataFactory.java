package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.entity.Address;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.v2.AddressDTO;

import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;

public class CardRequestTestDataFactory {

    private static final String CLAIM_ID = "6bfdbf4a-fb53-4fb6-ae3a-414a660bf3fc";

    public static CardRequest aValidCardRequest() {
        return CardRequest.builder()
                .address(aValidAddressDTO())
                .claimId(CLAIM_ID)
                .dateOfBirth(HOMER_DATE_OF_BIRTH)
                .firstName(HOMER_FORENAME)
                .lastName(SIMPSON_SURNAME)
                .email(HOMER_EMAIL)
                .mobile(HOMER_MOBILE)
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
