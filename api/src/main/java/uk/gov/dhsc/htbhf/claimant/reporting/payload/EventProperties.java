package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event properties with their associated field names for reporting to google analytics.
 * See https://developers.google.com/analytics/devguides/collection/analyticsjs/events
 */
@Getter
@AllArgsConstructor
public enum EventProperties {

    EVENT_CATEGORY("eventCategory"),
    EVENT_ACTION("eventAction"),
    EVENT_LABEL("eventLabel"),
    EVENT_VALUE("eventValue"),
    QUEUE_TIME("queueTime"),
    CUSTOMER_ID("cid");

    private String fieldName;
}
