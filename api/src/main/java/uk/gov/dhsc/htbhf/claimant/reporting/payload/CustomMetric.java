package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.Getter;

/**
 * Custom metrics with their associated indexes in google analytics.
 * Note, the indexes must be correct as these are used for reporting to google analytics measurement protocol.
 * See https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
 */
@Getter
public enum CustomMetric {

    CHILDREN_UNDER_ONE(1),
    CHILDREN_BETWEEN_ONE_AND_FOUR(2),
    PREGNANCIES(3),
    PAYMENT_FOR_CHILDREN_UNDER_ONE(4), // payments only
    PAYMENT_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR(5), // payments only
    PAYMENT_FOR_PREGNANCY(6), // payments only
    CLAIMANT_AGE(7),
    NUMBER_OF_1ST_OR_FOURTH_BIRTHDAYS_IN_NEXT_CYCLE(8), // payments only
    WEEKS_PREGNANT(9); // claim only

    private final String fieldName;

    CustomMetric(int index) {
        // Google analytics using the notation of 'cm'+index as the field name
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#cm_
        this.fieldName = "cm" + index;
    }
}
