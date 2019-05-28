package uk.gov.dhsc.htbhf.claimant.service.audit;

import lombok.Builder;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.BALANCE_ON_CARD;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.CLAIM_ID;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.ENTITLEMENT_AMOUNT_IN_PENCE;

public class BalanceTooHighForPaymentEvent extends Event {

    @Builder
    public BalanceTooHighForPaymentEvent(UUID claimId, int entitlementAmountInPence, int balanceOnCard) {
        super(ClaimEventType.BALANCE_TOO_HIGH_FOR_PAYMENT, LocalDateTime.now(), constructMetaData(claimId, entitlementAmountInPence, balanceOnCard));
    }

    private static Map<String, Object> constructMetaData(UUID claimId, int entitlementAmountInPence, int balanceOnCard) {
        return Map.of(
                CLAIM_ID.getKey(), claimId,
                ENTITLEMENT_AMOUNT_IN_PENCE.getKey(), entitlementAmountInPence,
                BALANCE_ON_CARD.getKey(), balanceOnCard);
    }
}
