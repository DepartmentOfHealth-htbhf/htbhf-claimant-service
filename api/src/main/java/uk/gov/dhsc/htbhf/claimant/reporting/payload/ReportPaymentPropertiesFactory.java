package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.NUMBER_OF_1ST_OR_FOURTH_BIRTHDAYS_IN_NEXT_CYCLE;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.PAYMENT_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.PAYMENT_FOR_CHILDREN_UNDER_ONE;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.CustomMetric.PAYMENT_FOR_PREGNANCY;
import static uk.gov.dhsc.htbhf.claimant.reporting.payload.EventCategory.PAYMENT;

@Component
public class ReportPaymentPropertiesFactory extends ReportPropertiesFactory {

    private final ChildDateOfBirthCalculator childDateOfBirthCalculator;

    public ReportPaymentPropertiesFactory(@Value("${google-analytics.tracking-id}") String trackingId,
                                          ClaimantCategoryCalculator claimantCategoryCalculator,
                                          ChildDateOfBirthCalculator childDateOfBirthCalculator) {
        super(trackingId, claimantCategoryCalculator);
        this.childDateOfBirthCalculator = childDateOfBirthCalculator;
    }

    public Map<String, String> createReportPropertiesForPaymentEvent(ReportPaymentMessageContext context) {
        Map<String, String> reportProperties = new LinkedHashMap<>();
        reportProperties.putAll(mapValuesToString(createMandatoryPropertiesMap()));
        int totalPaymentAmount = calculateTotalPaymentAmount(context);
        reportProperties.putAll(mapValuesToString(createEventPropertiesMap(context, PAYMENT, totalPaymentAmount)));
        reportProperties.putAll(mapValuesToString(createCommonCustomDimensions(context)));
        reportProperties.putAll(mapValuesToString(createCustomMetricMapForPaymentEvent(context)));
        return reportProperties;
    }

    private int calculateTotalPaymentAmount(ReportPaymentMessageContext context) {
        return context.getPaymentForPregnancy() + context.getPaymentForChildrenUnderOne()
                    + context.getPaymentForChildrenBetweenOneAndFour() + context.getPaymentForBackdatedVouchers();
    }

    private Map<String, Object> createCustomMetricMapForPaymentEvent(ReportPaymentMessageContext context) {
        Map<String, Object> customMetrics = createCommonCustomMetrics(context);
        addPaymentCycleMetrics(context, customMetrics);
        return customMetrics;
    }

    private void addPaymentCycleMetrics(ReportPaymentMessageContext context, Map<String, Object> customMetrics) {
        PaymentCycle paymentCycle = context.getPaymentCycle();
        customMetrics.put(PAYMENT_FOR_PREGNANCY.getFieldName(), context.getPaymentForPregnancy());
        customMetrics.put(PAYMENT_FOR_CHILDREN_UNDER_ONE.getFieldName(), context.getPaymentForChildrenUnderOne());
        customMetrics.put(PAYMENT_FOR_CHILDREN_BETWEEN_ONE_AND_FOUR.getFieldName(), context.getPaymentForChildrenBetweenOneAndFour());
        NextPaymentCycleSummary nextPaymentCycleSummary = childDateOfBirthCalculator.getNextPaymentCycleSummary(paymentCycle);
        customMetrics.put(NUMBER_OF_1ST_OR_FOURTH_BIRTHDAYS_IN_NEXT_CYCLE.getFieldName(), nextPaymentCycleSummary.getNumberOfChildrenTurningOneOrFour());
    }
}
