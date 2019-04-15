package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;

import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.testsupport.AddressDTOTestDataFactory.aValidAddressDTO;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.DATE_OF_BIRTH;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LAST_NAME;

public class CardRequestTestDataFactory {

    private static final String EMAIL = "test@email.com";
    private static final String MOBILE = "07700900000";

    public static CardRequest aValidCardRequest() {
        return CardRequest.builder()
                .address(aValidAddressDTO())
                .claimId(UUID.randomUUID().toString())
                .dateOfBirth(DATE_OF_BIRTH)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .email(EMAIL)
                .mobile(MOBILE)
                .build();

    }

}
