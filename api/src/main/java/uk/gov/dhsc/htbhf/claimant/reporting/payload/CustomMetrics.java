package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.Getter;

/**
 * Custom metrics with their associated indexes in google analytics.
 * Note, the indexes must be correct as these are used for reporting to google analytics.
 * See https://developers.google.com/analytics/devguides/collection/analyticsjs/custom-dims-mets
 */
@Getter
public enum CustomMetrics {

    CHILDREN_UNDER_ONE(1),
    CHILDREN_BETWEEN_ONE_AND_FOUR(2),
    PREGNANCIES(3),
    PAYMENT_FOR_CHILDREN_UNDER_ONE(4),
    PAYMENT_CHILDREN_BETWEEN_ONE_AND_FOUR(5),
    PAYMENT_FOR_PREGNANCY(6),
    CLAIMANT_AGE(7),
    NUMBER_OF_1ST_OR_FOURTH_BIRTHDAYS_IN_NEXT_CYCLE(8),
    WEEKS_PREGNANT(9);

    private final String fieldName;

    CustomMetrics(int index) {
        // Google analytics using the notation of 'metric'+index as the field name
        this.fieldName = "metric" + index;
    }
}
