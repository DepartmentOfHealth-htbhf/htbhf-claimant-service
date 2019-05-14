package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DetermineEntitlementMessagePayload implements MessagePayload {
    private UUID claimId;
    private UUID currentPaymentCycleId;
    private UUID previousPaymentCycleId;
}
