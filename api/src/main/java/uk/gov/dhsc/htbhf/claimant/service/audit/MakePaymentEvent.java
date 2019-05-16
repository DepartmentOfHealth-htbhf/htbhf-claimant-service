package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.PAYMENT_REFERENCE;

public class MakePaymentEvent extends Event {

    @Builder
    public MakePaymentEvent(UUID claimId, UUID paymentId, String reference) {
        super(ClaimEventType.MAKE_PAYMENT, LocalDateTime.now(), constructMetaData(claimId, paymentId, reference));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, UUID paymentId, String reference) {
        return Map.of(
                CLAIM_ID.getKey(), claimId,
                PAYMENT_ID.getKey(), paymentId,
                PAYMENT_REFERENCE.getKey(), reference
        );
    }
}
