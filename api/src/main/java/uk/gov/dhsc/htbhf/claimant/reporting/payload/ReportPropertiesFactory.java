package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomDimension.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.CLAIM;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventProperties.*;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.UserType.ONLINE;

/**
 * Factory class for creating a map of google analytic report properties.
 */
@Component
@RequiredArgsConstructor
public class ReportPropertiesFactory {

    private static final String HIT_TYPE_KEY = "hitType";
    private static final String HIT_TYPE_VALUE = "event";

    private final ClaimantCategoryCalculator claimantCategoryCalculator;

    public Map<String, Object> createReportPropertiesForClaimEvent(ReportClaimMessageContext context) {
        Map<String, Object> reportProperties = new HashMap<>();
        reportProperties.putAll(createEventPropertiesMap(context));
        reportProperties.putAll(createCustomDimensionMap(context));
        return reportProperties;
    }

    private Map<String, Object> createEventPropertiesMap(ReportClaimMessageContext context) {
        Map<String, Object> eventPropertiesMap = new HashMap<>();
        eventPropertiesMap.put(HIT_TYPE_KEY, HIT_TYPE_VALUE);
        eventPropertiesMap.put(EVENT_CATEGORY.getFieldName(), CLAIM.name());
        eventPropertiesMap.put(EVENT_ACTION.getFieldName(), context.getClaimAction().name());
        eventPropertiesMap.put(EVENT_VALUE.getFieldName(), 0);
        eventPropertiesMap.put(QUEUE_TIME.getFieldName(), calculateQueueTime(context.getTimestamp()));
        eventPropertiesMap.put(CUSTOMER_ID.getFieldName(), context.getClaim().getId());
        return eventPropertiesMap;
    }

    private Map<String, Object> createCustomDimensionMap(ReportClaimMessageContext context) {
        Map<String, Object> customDimensions = new HashMap<>();
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

    private Long calculateQueueTime(LocalDateTime timestamp) {
        return Duration.between(timestamp, LocalDateTime.now()).toMillis();
    }
}
