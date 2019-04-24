package uk.gov.dhsc.htbhf.claimant.testsupport;

import uk.gov.dhsc.htbhf.claimant.message.payload.NewCardRequestMessagePayload;

import java.util.UUID;

public class MessagePayloadTestDataFactory {

    private static final UUID CLAIM_ID = UUID.fromString("bc78da28-5bea-45fd-95ca-8bd82979c584");

    public static final String PAYLOAD_JSON = "{ \"claimId\":\"" + CLAIM_ID.toString() + "\"}";

    public static NewCardRequestMessagePayload aValidNewCardRequestMessagePayload() {
        return NewCardRequestMessagePayload.builder()
                .claimId(CLAIM_ID)
                .build();
    }
}
