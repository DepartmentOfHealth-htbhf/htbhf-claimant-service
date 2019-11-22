package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

public class CardResponseTestDataFactory {

    public static CardResponse aCardResponse() {
        return aCardResponse(CARD_ACCOUNT_ID);
    }

    public static CardResponse aCardResponse(String cardAccountId) {
        return CardResponse.builder()
                .cardAccountId(cardAccountId)
                .build();
    }
}
