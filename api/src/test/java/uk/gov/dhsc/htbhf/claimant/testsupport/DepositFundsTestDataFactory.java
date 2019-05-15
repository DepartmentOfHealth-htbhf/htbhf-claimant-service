package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;

public class DepositFundsTestDataFactory {

    public static DepositFundsRequest aValidDepositFundsRequest() {
        return DepositFundsRequest.builder()
                .amountInPence(123)
                .reference("My Payment reference")
                .build();
    }

    public static DepositFundsResponse aValidDepositFundsResponse() {
        return DepositFundsResponse.builder()
                .referenceId("A deposit funds reference")
                .build();
    }
}
