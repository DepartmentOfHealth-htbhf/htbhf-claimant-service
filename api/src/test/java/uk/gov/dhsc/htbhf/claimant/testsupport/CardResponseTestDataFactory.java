package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;

import java.util.UUID;

public class CardResponseTestDataFactory {

    public static CardResponse aCardResponse() {
        return CardResponse.builder()
                .cardAccountId(UUID.fromString("bc78da28-4918-45fd-95ca-8bd82979c584").toString())
                .build();
    }
}
