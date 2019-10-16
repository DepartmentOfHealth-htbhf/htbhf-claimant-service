package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The type of claimant, e.g. pregnant with no children, not pregnant with children, pregnant and under 16, etc.
 */
@Getter
@AllArgsConstructor
public enum ClaimantCategory {

    PREGNANT_AND_UNDER_16("Pregnant and under 16"),
    PREGNANT_AND_UNDER_18("Pregnant and under 18"),
    PREGNANT_WITH_NO_CHILDREN("Pregnant and no passported children"),
    PREGNANT_WITH_CHILDREN("Pregnant with passported children"),
    NOT_PREGNANT_WITH_CHILDREN("Not pregnant with passported children"),
    NOT_PREGNANT_WITH_NO_CHILDREN("Not pregnant with no passported children");

    private String description;
}
