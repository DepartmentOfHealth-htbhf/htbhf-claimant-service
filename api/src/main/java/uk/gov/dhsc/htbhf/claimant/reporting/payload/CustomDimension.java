package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.Getter;

/**
 * Custom dimensions with their associated indexes in google analytics.
 * Note, the indexes must be correct as these are used for reporting to google analytics measurement protocol.
 * See https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters
 */
@Getter
public enum CustomDimension {

    USER_TYPE(1),
    HELP_DESK_USER_ID(2),
    CLAIMANT_CATEGORY(3),
    LOCAL_AUTHORITY(4),
    LOCAL_AUTHORITY_CODE(5),
    COUNTRY(6),
    POSTCODE_OUTCODE(7),
    WESTMINSTER_PARLIAMENTARY_CONSTITUENCY(8),
    CLINICAL_COMMISSIONING_GROUP(9),
    CLINICAL_COMMISSIONING_GROUP_CODE(10),
    NUMBER_OF_WEEKS_PREGNANT(11),
    QUALIFYING_BENEFIT(12);

    private final String fieldName;

    CustomDimension(int index) {
        // Google analytics using the notation of 'cd'+index as the field name
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#customs
        this.fieldName = "cd" + index;
    }
}
