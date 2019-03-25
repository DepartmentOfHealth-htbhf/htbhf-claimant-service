package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The possible states that a claim can be in.
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum EligibilityStatus {

    ELIGIBLE,
    INELIGIBLE,
    PENDING,
    NOMATCH,
    ERROR,
    DUPLICATE
}
