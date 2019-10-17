package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomDimension.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.CLAIM;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventProperties.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.UserType.ONLINE;

/**
 * Factory class for creating a map of parameters reported to google analytics measurement protocol.
 */
@Component
@RequiredArgsConstructor
public class ReportPropertiesFactory {

    private static final String HIT_TYPE_KEY = "t";
    private static final String HIT_TYPE_VALUE = "event";
    // queue times longer than 4 hours may cause the event to not be registered:
    // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
    private static final Long MAX_QUEUE_TIME = 1000L * 60 * 60 * 4;

    private final ClaimantCategoryCalculator claimantCategoryCalculator;

    public Map<String, String> createReportPropertiesForClaimEvent(ReportClaimMessageContext context) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        reportProperties.putAll(mapValuesToString(createEventPropertiesMap(context)));
        reportProperties.putAll(mapValuesToString(createCustomDimensionMap(context)));
        reportProperties.putAll(mapValuesToString(createCustomMetricMap(context)));
        return reportProperties;
    }

    private Map<String, Object> createEventPropertiesMap(ReportClaimMessageContext context) {
        Map<String, Object> eventPropertiesMap = new LinkedHashMap<>();
        eventPropertiesMap.put(HIT_TYPE_KEY, HIT_TYPE_VALUE);
        eventPropertiesMap.put(EVENT_CATEGORY.getFieldName(), CLAIM.name());
        eventPropertiesMap.put(EVENT_ACTION.getFieldName(), context.getClaimAction().name());
        eventPropertiesMap.put(EVENT_VALUE.getFieldName(), 0);
        eventPropertiesMap.put(QUEUE_TIME.getFieldName(), calculateQueueTime(context.getTimestamp()));
        eventPropertiesMap.put(CUSTOMER_ID.getFieldName(), context.getClaim().getId());
        return eventPropertiesMap;
    }

    private Map<String, Object> createCustomDimensionMap(ReportClaimMessageContext context) {
        Map<String, Object> customDimensions = new TreeMap<>();
        customDimensions.put(USER_TYPE.getFieldName(), ONLINE.name());
        ClaimantCategory claimantCategory = claimantCategoryCalculator
                .determineClaimantCategory(context.getClaim().getClaimant(), context.getDatesOfBirthOfChildren(), context.getTimestamp().toLocalDate());
        customDimensions.put(CLAIMANT_CATEGORY.getFieldName(), claimantCategory.getDescription());
        PostcodeData postcodeData = context.getClaim().getPostcodeData();
        customDimensions.put(LOCAL_AUTHORITY.getFieldName(), postcodeData.getAdminDistrict());
        customDimensions.put(LOCAL_AUTHORITY_CODE.getFieldName(), postcodeData.getCodes().getAdminDistrict());
        customDimensions.put(COUNTRY.getFieldName(), postcodeData.getCountry());
        customDimensions.put(POSTCODE_OUTCODE.getFieldName(), postcodeData.getOutcode());
        customDimensions.put(WESTMINSTER_PARLIAMENTARY_CONSTITUENCY.getFieldName(), postcodeData.getParliamentaryConstituency());
        customDimensions.put(CLINICAL_COMMISSIONING_GROUP.getFieldName(), postcodeData.getCcg());
        customDimensions.put(CLINICAL_COMMISSIONING_GROUP_CODE.getFieldName(), postcodeData.getCodes().getCcg());
        return customDimensions;
    }

    private Map<String, Object> createCustomMetricMap(ReportClaimMessageContext context) {
        Map<String, Object> customMetrics = new TreeMap<>();
        Claimant claimant = context.getClaim().getClaimant();
        LocalDate atDate = context.getTimestamp().toLocalDate();
        customMetrics.put(CLAIMANT_AGE.getFieldName(), Period.between(claimant.getDateOfBirth(), atDate).getYears());
        long childrenUnder4 = context.getDatesOfBirthOfChildren().stream().filter(dob -> dob.isAfter(atDate.minusYears(4))).count();
        long childrenUnder1 = context.getDatesOfBirthOfChildren().stream().filter(dob -> dob.isAfter(atDate.minusYears(1))).count();
        customMetrics.put(CHILDREN_UNDER_ONE.getFieldName(), childrenUnder1);
        customMetrics.put(CHILDREN_BETWEEN_ONE_AND_FOUR.getFieldName(), childrenUnder4 - childrenUnder1);
        LocalDate expectedDeliveryDate = claimant.getExpectedDeliveryDate();
        if (expectedDeliveryDate == null || expectedDeliveryDate.isBefore(atDate)) {
            customMetrics.put(PREGNANCIES.getFieldName(), 0);
        } else {
            customMetrics.put(PREGNANCIES.getFieldName(), 1);
            LocalDate conception = expectedDeliveryDate.minusMonths(9);
            customMetrics.put(WEEKS_PREGNANT.getFieldName(), ChronoUnit.WEEKS.between(conception, atDate));
        }
        return customMetrics;
    }

    private Long calculateQueueTime(LocalDateTime timestamp) {
        long queueTime = Duration.between(timestamp, LocalDateTime.now()).toMillis();
        return Math.min(queueTime, MAX_QUEUE_TIME);
    }

    private Map<String, String> mapValuesToString(Map<String, Object> original) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        original.forEach((key, value) -> reportProperties.put(key, String.valueOf(value)));
        return reportProperties;
    }
}
