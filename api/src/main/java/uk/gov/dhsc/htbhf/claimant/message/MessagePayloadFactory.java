package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

public class MessagePayloadFactory {

    //TODO MRS 2019-04-24: Unit test
    public NewCardRequestMessagePayload buildNewCardMessagePayload(Claim claim) {
        return NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .build();
    }
}
