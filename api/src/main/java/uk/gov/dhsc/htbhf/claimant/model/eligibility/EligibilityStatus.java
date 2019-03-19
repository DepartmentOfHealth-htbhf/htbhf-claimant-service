package uk.gov.dhsc.htbhf.claimant.model.eligibility;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;

/**
 * The possible states that a claim can be in according to the DWP.
 * The value of ERROR has been added for when there are problems connecting to DWP.
 */
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum EligibilityStatus {

    ELIGIBLE,
    INELIGIBLE,
    PENDING,
    NOMATCH,
    ERROR
}
