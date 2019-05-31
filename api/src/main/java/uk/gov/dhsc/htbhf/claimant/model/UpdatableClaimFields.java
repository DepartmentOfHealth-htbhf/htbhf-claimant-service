package uk.gov.dhsc.htbhf.claimant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UpdatableClaimFields {

    EXPECTED_DELIVERY_DATE("expectedDeliveryDate");

    private final String fieldName;
}
