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
    private static final String CLAIM_ID = "6bfdbf4a-fb53-4fb6-ae3a-414a660bf3fc";

    public static CardRequest aValidCardRequest() {
        return CardRequest.builder()
                .address(aValidAddressDTO())
                .claimId(UUID.fromString(CLAIM_ID).toString())
                .dateOfBirth(DATE_OF_BIRTH)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .email(EMAIL)
                .mobile(MOBILE)
                .build();

    }

}
