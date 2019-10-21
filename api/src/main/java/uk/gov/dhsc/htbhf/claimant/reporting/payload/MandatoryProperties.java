package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Key names for google analytics mandatory properties.
 * Note, this does not include customer id which is covered in the {@link EventProperties} enum.
 * See https://developers.google.com/analytics/devguides/collection/protocol/v1/reference#required
 */
@Getter
@AllArgsConstructor
public enum MandatoryProperties {

    HIT_TYPE_KEY("t"),
    PROTOCOL_VERSION_KEY("v"),
    TRACKING_ID_KEY("tid");

    private String fieldName;
}
