package uk.gov.dhsc.htbhf.claimant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of all the fields on a Claimant that may be updated by the claimant.
 */
@AllArgsConstructor
@Getter
public enum UpdatableClaimantFields {

    EXPECTED_DELIVERY_DATE("expectedDeliveryDate");

    private final String fieldName;
}
