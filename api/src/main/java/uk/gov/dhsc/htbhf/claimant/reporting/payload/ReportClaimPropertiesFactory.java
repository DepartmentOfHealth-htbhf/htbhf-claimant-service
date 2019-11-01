package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.WEEKS_PREGNANT;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.CLAIM;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventProperties.EVENT_LABEL;

/**
 * Factory class for creating a map of parameters for claims reported to google analytics measurement protocol.
 */
@Component
public class ReportClaimPropertiesFactory extends ReportPropertiesFactory {

    public ReportClaimPropertiesFactory(@Value("${google-analytics.tracking-id}") String trackingId,
                                        ClaimantCategoryCalculator claimantCategoryCalculator) {
        super(trackingId, claimantCategoryCalculator);
    }

    public Map<String, String> createReportPropertiesForClaimEvent(ReportClaimMessageContext context) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        reportProperties.putAll(mapValuesToString(createMandatoryPropertiesMap()));
        reportProperties.putAll(mapValuesToString(createEventPropertiesMap(context, CLAIM, 0)));
        if (updatedClaimFieldsExists(context)) {
            reportProperties.put(EVENT_LABEL.getFieldName(), convertToCommaSeparatedString(context.getUpdatedClaimantFields()));
        }
        reportProperties.putAll(mapValuesToString(createCustomDimensionMap(context)));
        reportProperties.putAll(mapValuesToString(createCustomMetricMapForClaimEvent(context)));
        return reportProperties;
    }

    private boolean updatedClaimFieldsExists(ReportClaimMessageContext context) {
        return !isEmpty(context.getUpdatedClaimantFields());
    }

    private String convertToCommaSeparatedString(List<UpdatableClaimantField> updatedClaimFields) {
        return updatedClaimFields.stream().map(UpdatableClaimantField::getFieldName).collect(Collectors.joining(", "));
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
}
