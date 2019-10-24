package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportEventMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;
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

import static uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomDimension.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.CLAIM;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventProperties.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.HIT_TYPE_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.PROTOCOL_VERSION_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.TRACKING_ID_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.UserType.ONLINE;

/**
 * Factory class for creating a map of parameters reported to google analytics measurement protocol.
 */
@Component
@SuppressWarnings("PMD.TooManyMethods")
public class ReportPropertiesFactory {

    private static final String HIT_TYPE_VALUE = "event";
    private static final String PROTOCOL_VERSION_VALUE = "1";
    // queue times longer than 4 hours may cause the event to not be registered:
    // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
    private static final Long MAX_QUEUE_TIME = 1000L * 60 * 60 * 4;
    public static final int PREGNANCY_DURATION_IN_WEEKS = 40;

    private final ClaimantCategoryCalculator claimantCategoryCalculator;
    private final ChildDateOfBirthCalculator childDateOfBirthCalculator;
    private final String trackingId;

    public ReportPropertiesFactory(@Value("${google-analytics.tracking-id}") String trackingId,
                                   ClaimantCategoryCalculator claimantCategoryCalculator,
                                   ChildDateOfBirthCalculator childDateOfBirthCalculator) {
        this.trackingId = trackingId;
        this.claimantCategoryCalculator = claimantCategoryCalculator;
        this.childDateOfBirthCalculator = childDateOfBirthCalculator;
    }

    public Map<String, String> createReportPropertiesForClaimEvent(ReportClaimMessageContext context) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        reportProperties.putAll(mapValuesToString(createMandatoryPropertiesMap()));
        reportProperties.putAll(mapValuesToString(createEventPropertiesMap(context, CLAIM, 0)));
        reportProperties.putAll(mapValuesToString(createCustomDimensionMap(context)));
        reportProperties.putAll(mapValuesToString(createCustomMetricMapForClaimEvent(context)));
        return reportProperties;
    }

    public Map<String, String> createReportPropertiesForPaymentEvent(ReportPaymentMessageContext context) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        reportProperties.putAll(mapValuesToString(createMandatoryPropertiesMap()));
        int totalPaymentAmount = context.getPaymentForPregnancy() + context.getPaymentForChildrenUnderOne() + context.getPaymentForChildrenBetweenOneAndFour();
        reportProperties.putAll(mapValuesToString(createEventPropertiesMap(context, PAYMENT, totalPaymentAmount)));
        reportProperties.putAll(mapValuesToString(createCustomDimensionMap(context)));
        reportProperties.putAll(mapValuesToString(createCustomMetricMapForPaymentEvent(context)));
        return reportProperties;
    }

    private Map<String, Object> createMandatoryPropertiesMap() {
        Map<String, Object> mandatoryProperties = new LinkedHashMap<>();
        mandatoryProperties.put(HIT_TYPE_KEY.getFieldName(), HIT_TYPE_VALUE);
        mandatoryProperties.put(PROTOCOL_VERSION_KEY.getFieldName(), PROTOCOL_VERSION_VALUE);
        mandatoryProperties.put(TRACKING_ID_KEY.getFieldName(), trackingId);
        return mandatoryProperties;
    }

    private Map<String, Object> createEventPropertiesMap(ReportEventMessageContext context, EventCategory eventCategory, int eventValue) {
        Map<String, Object> eventPropertiesMap = new LinkedHashMap<>();
        eventPropertiesMap.put(EVENT_CATEGORY.getFieldName(), eventCategory.name());
        eventPropertiesMap.put(EVENT_ACTION.getFieldName(), context.getEventAction());
        eventPropertiesMap.put(EVENT_VALUE.getFieldName(), eventValue);
        eventPropertiesMap.put(QUEUE_TIME.getFieldName(), calculateQueueTime(context.getTimestamp()));
        eventPropertiesMap.put(CUSTOMER_ID.getFieldName(), context.getClaim().getId());
        return eventPropertiesMap;
    }

    private Map<String, Object> createCustomDimensionMap(ReportEventMessageContext context) {
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

    private Map<String, Object> createCustomMetricMapForClaimEvent(ReportClaimMessageContext context) {
        Map<String, Object> customMetrics = createCommonCustomMetrics(context);
        LocalDate expectedDeliveryDate = context.getClaim().getClaimant().getExpectedDeliveryDate();
        LocalDate atDate = context.getTimestamp().toLocalDate();
        if (isClaimantPregnant(expectedDeliveryDate, atDate)) {
            LocalDate conception = expectedDeliveryDate.minusWeeks(PREGNANCY_DURATION_IN_WEEKS);
            customMetrics.put(WEEKS_PREGNANT.getFieldName(), ChronoUnit.WEEKS.between(conception, atDate));
        }
        return customMetrics;
    }

    private Map<String, Object> createCustomMetricMapForPaymentEvent(ReportPaymentMessageContext context) {
        Map<String, Object> customMetrics = createCommonCustomMetrics(context);
        addPaymentCycleMetrics(context, customMetrics);
        return customMetrics;
    }

    private Map<String, Object> createCommonCustomMetrics(ReportEventMessageContext context) {
        Map<String, Object> customMetrics = new TreeMap<>();
        Claimant claimant = context.getClaim().getClaimant();
        LocalDate atDate = context.getTimestamp().toLocalDate();
        customMetrics.put(CLAIMANT_AGE.getFieldName(), Period.between(claimant.getDateOfBirth(), atDate).getYears());
        long childrenUnder4 = getNumberOfChildrenUnderFour(context.getDatesOfBirthOfChildren(), context.getTimestamp().toLocalDate());
        long childrenUnder1 = getNumberOfChildrenUnderOne(context.getDatesOfBirthOfChildren(), context.getTimestamp().toLocalDate());
        customMetrics.put(CHILDREN_UNDER_ONE.getFieldName(), childrenUnder1);
        customMetrics.put(CHILDREN_BETWEEN_ONE_AND_FOUR.getFieldName(), childrenUnder4 - childrenUnder1);
        LocalDate expectedDeliveryDate = claimant.getExpectedDeliveryDate();
        int pregnanciesValue = isClaimantPregnant(expectedDeliveryDate, atDate) ? 1 : 0;
        customMetrics.put(PREGNANCIES.getFieldName(), pregnanciesValue);
        return customMetrics;
    }

    private boolean isClaimantPregnant(LocalDate expectedDeliveryDate, LocalDate atDate) {
        return expectedDeliveryDate != null && !expectedDeliveryDate.isBefore(atDate);
    }

    private void addPaymentCycleMetrics(ReportPaymentMessageContext context, Map<String, Object> customMetrics) {
        PaymentCycle paymentCycle = context.getPaymentCycle();
        customMetrics.put(PAYMENT_FOR_PREGNANCY.getFieldName(), context.getPaymentForPregnancy());
        customMetrics.put(PAYMENT_FOR_CHILDREN_UNDER_ONE.getFieldName(), context.getPaymentForChildrenUnderOne());
        customMetrics.put(PAYMENT_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR.getFieldName(), context.getPaymentForChildrenBetweenOneAndFour());
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        customMetrics.put(NUMBER_OF_1ST_OR_FOURTH_BIRTHDAYS_IN_NEXT_CYCLE.getFieldName(), nextPaymentCycleSummary.getNumberOfChildrenTurningOneOrFour());
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
