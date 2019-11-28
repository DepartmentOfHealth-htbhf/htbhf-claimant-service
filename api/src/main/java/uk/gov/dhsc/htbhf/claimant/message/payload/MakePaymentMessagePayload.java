package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class MakePaymentMessagePayload implements MessagePayload {
    private UUID claimId;
    private UUID paymentCycleId;
    private String cardAccountId;
    private boolean paymentRestarted;
}
