package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithDueDateAndPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.dwp.model.QualifyingReason.UNIVERSAL_CREDIT;
import static uk.gov.dhsc.htbhf.eligibility.model.testhelper.CombinedIdAndEligibilityResponseTestDataFactory.anIdMatchedEligibilityConfirmedUCResponseWithAllMatches;

@ExtendWith(MockitoExtension.class)
class ReportPaymentPropertiesFactoryTest extends ReportPropertiesFactoryTest {

    @Mock
    private ClaimantCategoryCalculator claimantCategoryCalculator;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    private ReportPaymentPropertiesFactory reportClaimPropertiesFactory;

    @BeforeEach
    void init() {
        reportClaimPropertiesFactory = new ReportPaymentPropertiesFactory(TRACKING_ID, claimantCategoryCalculator, childDateOfBirthCalculator);
    }

    @Test
    void shouldCreateReportPropertiesForPayment() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportPaymentMessageContext context = aReportPaymentMessageContext(timestamp, datesOfBirthOfChildren, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(context.getPaymentCycle()))
                .willReturn(NextPaymentCycleSummary.builder()
                        .numberOfChildrenTurningOne(1)
                        .numberOfChildrenTurningFour(1)
                        .build());

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForPaymentEvent(context);

        assertQueueTime(reportProperties, timestamp, secondsSinceEvent);
        Claim claim = context.getClaim();
        assertCommonProperties(reportProperties, timestamp, claim, "PAYMENT", "SCHEDULED_PAYMENT");
        assertThat(reportProperties).contains(
                entry("ev", "400"), // event value is the total payment amount
                entry("cm4", "100"), // payment for children under one (one child for cycle = 8 vouchers)
                entry("cm5", "100"), // payment for children between one and four (one child for cycle = 4 vouchers)
                entry("cm6", "100"), // payment for pregnancy (pregnant for the entire payment cycle = 4 vouchers)
                entry("cm8", "2") // number of children turning 1 or 4 in the next cycle.
        );
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate(), UNIVERSAL_CREDIT);
        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(context.getPaymentCycle());
    }

    private ReportPaymentMessageContext aReportPaymentMessageContext(LocalDateTime timestamp,
                                                                     List<LocalDate> datesOfBirthOfChildren,
                                                                     LocalDate expectedDeliveryDate) {
        Claim claim = aClaimWithDueDateAndPostcodeData(expectedDeliveryDate);
        return ReportPaymentMessageContext.builder()
                .paymentAction(SCHEDULED_PAYMENT)
                .paymentCycle(aPaymentCycleWithClaim(claim))
                .paymentForPregnancy(100)
                .paymentForChildrenUnderOne(100)
                .paymentForChildrenBetweenOneAndFour(100)
                .paymentForBackdatedVouchers(100)
                .claim(claim)
                .identityAndEligibilityResponse(anIdMatchedEligibilityConfirmedUCResponseWithAllMatches(datesOfBirthOfChildren))
                .timestamp(timestamp)
                .build();
    }
}
