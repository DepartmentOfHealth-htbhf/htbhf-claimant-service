package uk.gov.dhsc.htbhf.claimant.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;

/**
 * The possible values for the status of a claim.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@ApiModel(description = "The claim status")
public enum ClaimStatus {
    REJECTED,
    NEW,
    PENDING,
    ACTIVE,
    PENDING_EXPIRY,
    EXPIRED,
    ERROR
}
