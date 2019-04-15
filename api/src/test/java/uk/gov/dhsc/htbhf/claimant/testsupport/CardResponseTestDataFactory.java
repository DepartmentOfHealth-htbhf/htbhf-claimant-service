package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;

import java.util.UUID;

public class CardResponseTestDataFactory {

    public static CardResponse aCardResponse() {
        return CardResponse.builder()
                .cardAccountId(UUID.randomUUID().toString())
                .build();
    }
}
