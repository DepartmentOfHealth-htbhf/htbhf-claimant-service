package uk.gov.dhsc.htbhf.claimant.model;

/**
 * The possible values for the status of a claim.
 */
public enum ClaimStatus {
    REJECTED,
    NEW,
    PENDING,
    ACTIVE,
    PENDING_EXPIRY,
    EXPIRED,
    ERROR
}
