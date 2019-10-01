package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The possible values for the status of a claim.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ClaimStatus {
    REJECTED,
    NEW,
    PENDING,
    ACTIVE,
    PENDING_EXPIRY,
    EXPIRED,
    ERROR
}
