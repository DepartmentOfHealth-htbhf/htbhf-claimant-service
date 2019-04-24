package uk.gov.dhsc.htbhf.claimant.message;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

public class MessagePayloadFactory {

    public static NewCardRequestMessagePayload buildNewCardMessagePayload(Claim claim) {
        return NewCardRequestMessagePayload.builder()
                .claimId(claim.getId())
                .build();
    }
}
