package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;

public class CardResponseTestDataFactory {

    public static CardResponse aCardResponse() {
        return aCardResponse("bc78da28-4918-45fd-95ca-8bd82979c584");
    }

    public static CardResponse aCardResponse(String cardAccountId) {
        return CardResponse.builder()
                .cardAccountId(cardAccountId)
                .build();
    }
}
