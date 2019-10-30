package uk.gov.dhsc.htbhf.claimant.reporting;

/**
 * Action that triggered a claim event.
 */
public enum ClaimAction {
    NEW,
    REJECTED,
    UPDATED,
    UPDATED_FROM_NEW_TO_ACTIVE,
    UPDATED_FROM_ACTIVE_TO_PENDING_EXPIRY,
    UPDATED_FROM_ACTIVE_TO_EXPIRED,
    UPDATED_FROM_PENDING_EXPIRY_TO_EXPIRED,
    UPDATED_FROM_PENDING_EXPIRY_TO_ACTIVE
}
