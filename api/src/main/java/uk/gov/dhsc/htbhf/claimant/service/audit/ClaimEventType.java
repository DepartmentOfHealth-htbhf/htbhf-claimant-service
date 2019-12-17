package uk.gov.dhsc.htbhf.claimant.service.audit;

import uk.gov.dhsc.htbhf.logging.EventType;

/**
 * Enum storing the types of events created by the claimant-service.
 */
public enum ClaimEventType implements EventType {
    NEW_CLAIM,
    UPDATED_CLAIM,
    NEW_CARD,
    MAKE_PAYMENT,
    BALANCE_TOO_HIGH_FOR_PAYMENT,
    FAILED_EMAIL,
    FAILED_LETTER,
    EXPIRED_CLAIM
}
