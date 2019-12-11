package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportEventMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator.getNumberOfChildrenUnderFour;
import static uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator.getNumberOfChildrenUnderOne;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomDimension.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.CHILDREN_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.CLAIMANT_AGE;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.PREGNANCIES;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventProperties.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.HIT_TYPE_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.PROTOCOL_VERSION_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.MandatoryProperties.TRACKING_ID_KEY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.UserType.ONLINE;

/**
 * Factory class for creating a map of parameters reported to google analytics measurement protocol.
 */
public abstract class ReportPropertiesFactory {

    private static final String HIT_TYPE_VALUE = "event";
    private static final String PROTOCOL_VERSION_VALUE = "1";
    // queue times longer than 4 hours may cause the event to not be registered:
    // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
    private static final Long MAX_QUEUE_TIME = 1000L * 60 * 60 * 4;
    public static final int PREGNANCY_DURATION_IN_WEEKS = 40;

    private final ClaimantCategoryCalculator claimantCategoryCalculator;
    private final String trackingId;

    public ReportPropertiesFactory(String trackingId,
                                   ClaimantCategoryCalculator claimantCategoryCalculator) {
        this.trackingId = trackingId;
        this.claimantCategoryCalculator = claimantCategoryCalculator;
    }

    protected Long calculateQueueTime(LocalDateTime timestamp) {
        long queueTime = Duration.between(timestamp, LocalDateTime.now()).toMillis();
        return Math.min(queueTime, MAX_QUEUE_TIME);
    }

    protected Map<String, String> mapValuesToString(Map<String, Object> original) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        original.forEach((key, value) -> reportProperties.put(key, String.valueOf(value)));
        return reportProperties;
    }

    protected Map<String, Object> createMandatoryPropertiesMap() {
        Map<String, Object> mandatoryProperties = new LinkedHashMap<>();
        mandatoryProperties.put(HIT_TYPE_KEY.getFieldName(), HIT_TYPE_VALUE);
        mandatoryProperties.put(PROTOCOL_VERSION_KEY.getFieldName(), PROTOCOL_VERSION_VALUE);
        mandatoryProperties.put(TRACKING_ID_KEY.getFieldName(), trackingId);
        return mandatoryProperties;
    }

    protected Map<String, Object> createEventPropertiesMap(ReportEventMessageContext context, EventCategory eventCategory, int eventValue) {
        Map<String, Object> eventPropertiesMap = new LinkedHashMap<>();
        eventPropertiesMap.put(EVENT_CATEGORY.getFieldName(), eventCategory.name());
        eventPropertiesMap.put(EVENT_ACTION.getFieldName(), context.getEventAction());
        eventPropertiesMap.put(EVENT_VALUE.getFieldName(), eventValue);
        eventPropertiesMap.put(QUEUE_TIME.getFieldName(), calculateQueueTime(context.getTimestamp()));
        eventPropertiesMap.put(CUSTOMER_ID.getFieldName(), context.getClaim().getId());
        return eventPropertiesMap;
    }

    protected Map<String, Object> createCommonCustomDimensions(ReportEventMessageContext context) {
        Map<String, Object> customDimensions = new TreeMap<>();
        customDimensions.put(USER_TYPE.getFieldName(), ONLINE.name());
        Claim claim = context.getClaim();
        List<LocalDate> dobOfChildrenUnder4 = context.getIdentityAndEligibilityResponse().getDobOfChildrenUnder4();
        ClaimantCategory claimantCategory = claimantCategoryCalculator
                .determineClaimantCategory(claim.getClaimant(), dobOfChildrenUnder4, context.getTimestamp().toLocalDate());

        customDimensions.put(CLAIMANT_CATEGORY.getFieldName(), claimantCategory.getDescription());
        PostcodeData postcodeData = claim.getPostcodeData();
        customDimensions.put(LOCAL_AUTHORITY.getFieldName(), postcodeData.getAdminDistrict());
        customDimensions.put(LOCAL_AUTHORITY_CODE.getFieldName(), postcodeData.getCodes().getAdminDistrict());
        customDimensions.put(COUNTRY.getFieldName(), postcodeData.getCountry());
        customDimensions.put(POSTCODE_OUTCODE.getFieldName(), postcodeData.getOutcode());
        customDimensions.put(WESTMINSTER_PARLIAMENTARY_CONSTITUENCY.getFieldName(), postcodeData.getParliamentaryConstituency());
        customDimensions.put(CLINICAL_COMMISSIONING_GROUP.getFieldName(), postcodeData.getCcg());
        customDimensions.put(CLINICAL_COMMISSIONING_GROUP_CODE.getFieldName(), postcodeData.getCodes().getCcg());
        customDimensions.put(QUALIFYING_BENEFIT.getFieldName(), claim.getCurrentIdentityAndEligibilityResponse().getQualifyingBenefits());

        return customDimensions;
    }

    protected Map<String, Object> createCommonCustomMetrics(ReportEventMessageContext context) {
        Map<String, Object> customMetrics = new TreeMap<>();
        Claimant claimant = context.getClaim().getClaimant();
        LocalDate atDate = context.getTimestamp().toLocalDate();
        customMetrics.put(CLAIMANT_AGE.getFieldName(), Period.between(claimant.getDateOfBirth(), atDate).getYears());
        List<LocalDate> dobOfChildrenUnder4 = context.getIdentityAndEligibilityResponse().getDobOfChildrenUnder4();
        long childrenUnder4 = getNumberOfChildrenUnderFour(dobOfChildrenUnder4, atDate);
        long childrenUnder1 = getNumberOfChildrenUnderOne(dobOfChildrenUnder4, atDate);
        customMetrics.put(CHILDREN_UNDER_ONE.getFieldName(), childrenUnder1);
        customMetrics.put(CHILDREN_BETWEEN_ONE_AND_FOUR.getFieldName(), childrenUnder4 - childrenUnder1);
        LocalDate expectedDeliveryDate = claimant.getExpectedDeliveryDate();
        int pregnanciesValue = isClaimantPregnant(expectedDeliveryDate, atDate) ? 1 : 0;
        customMetrics.put(PREGNANCIES.getFieldName(), pregnanciesValue);
        return customMetrics;
    }

    protected boolean isClaimantPregnant(LocalDate expectedDeliveryDate, LocalDate atDate) {
        return expectedDeliveryDate != null && !expectedDeliveryDate.isBefore(atDate);
    }
}
