package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;

import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.AVAILABLE_BALANCE_IN_PENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.LEDGER_BALANCE_IN_PENCE;

public class CardBalanceResponseTestDataFactory {

    public static CardBalanceResponse aValidCardBalanceResponse() {
        return CardBalanceResponse.builder()
                .availableBalanceInPence(AVAILABLE_BALANCE_IN_PENCE)
                .ledgerBalanceInPence(LEDGER_BALANCE_IN_PENCE)
                .build();
    }
}
