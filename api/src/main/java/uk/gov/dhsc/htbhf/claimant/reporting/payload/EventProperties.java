package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event properties with their associated field names for reporting to the google analytics measurement protocol.
 * See https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
 */
@Getter
@AllArgsConstructor
public enum EventProperties {

    EVENT_CATEGORY("ec"),
    EVENT_ACTION("ea"),
    EVENT_LABEL("el"),
    EVENT_VALUE("ev"),
    QUEUE_TIME("qt"),
    CUSTOMER_ID("cid");

    private String fieldName;
}
