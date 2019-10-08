package uk.gov.dhsc.htbhf.claimant.message.payload;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AdditionalPregnancyPaymentMessagePayload implements MessagePayload {
    private UUID claimId;

    public static AdditionalPregnancyPaymentMessagePayload withClaimId(UUID claimId) {
        return new AdditionalPregnancyPaymentMessagePayload(claimId);
    }
}
