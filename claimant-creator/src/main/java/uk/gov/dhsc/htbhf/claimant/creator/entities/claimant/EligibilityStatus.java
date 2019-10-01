package uk.gov.dhsc.htbhf.claimant.creator.entities.claimant;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The possible values for the eligibility of a claimant.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum EligibilityStatus {
    ELIGIBLE,
    INELIGIBLE,
    PENDING,
    NO_MATCH,
    ERROR,
    DUPLICATE
}
