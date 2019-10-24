package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.processor.ChildDateOfBirthCalculator;
import uk.gov.dhsc.htbhf.claimant.message.processor.NextPaymentCycleSummary;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.NEW;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithDueDateAndPostcodeData;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class ReportPropertiesFactoryTest {

    private static final String TRACKING_ID = "tracking-id";
    private static final ClaimantCategory CLAIMANT_CATEGORY = ClaimantCategory.PREGNANT_WITH_CHILDREN;

    @Mock
    private ClaimantCategoryCalculator claimantCategoryCalculator;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;

    private ReportPropertiesFactory reportPropertiesFactory;

    @BeforeEach
    void init() {
        reportPropertiesFactory = new ReportPropertiesFactory(TRACKING_ID, claimantCategoryCalculator, childDateOfBirthCalculator);
    }

    @Test
    void shouldCreateReportPropertiesForClaim() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportClaimMessageContext context = aReportClaimMessageContext(timestamp, datesOfBirthOfChildren, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertQueueTime(reportProperties, timestamp, secondsSinceEvent);
        Claim claim = context.getClaim();
        assertCommonProperties(reportProperties, timestamp, claim, "CLAIM", "NEW");
        assertThat(reportProperties).contains(
                entry("ev", "0"), // event value (set to 0 as it's not used for claim events)
                entry("cm9", getNumberOfWeeksPregnant(claim, timestamp))); // weeks pregnant
        assertThat(reportProperties).doesNotContainKeys("cm4", "cm5", "cm6", "cm8"); // payment-only custom metrics
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate());
    }

    @Test
    void shouldCreateReportPropertiesForPayment() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportPaymentMessageContext context = aReportPaymentMessageContext(timestamp, datesOfBirthOfChildren, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);
        given(childDateOfBirthCalculator.getNextPaymentCycleSummary(context.getPaymentCycle()))
                .willReturn(NextPaymentCycleSummary.builder()
                        .numberOfChildrenTurningOne(1)
                        .numberOfChildrenTurningFour(1)
                        .build());

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForPaymentEvent(context);

        assertQueueTime(reportProperties, timestamp, secondsSinceEvent);
        Claim claim = context.getClaim();
        assertCommonProperties(reportProperties, timestamp, claim, "PAYMENT", "SCHEDULED_PAYMENT");
        assertThat(reportProperties).contains(
                entry("ev", "300"), // event value is the total payment amount
                entry("cm4", "100"), // payment for children under one (one child for cycle = 8 vouchers)
                entry("cm5", "100"), // payment for children between one and four (one child for cycle = 4 vouchers)
                entry("cm6", "100"), // payment for pregnancy (pregnant for the entire payment cycle = 4 vouchers)
                entry("cm8", "2") // number of children turning 1 or 4 in the next cycle.
        );
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate());
        verify(childDateOfBirthCalculator).getNextPaymentCycleSummary(context.getPaymentCycle());
    }

    @Test
    void shouldImposeMaximumValueForQueueTime() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now().minusHours(5), NO_CHILDREN, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        // queue times longer than 4 hours may cause the event to not be registered:
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
        Long queueTime = Long.parseLong(reportProperties.get("qt"));
        long maxQueueTime = 1000 * 60 * 60 * 4;
        assertThat(queueTime).isLessThanOrEqualTo(maxQueueTime);
    }

    @Test
    void shouldIncludeMetricsForZeroChildren() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm1", "0"),
                entry("cm2", "0")
        );
    }

    @ParameterizedTest(name = "{1} children under 1 and {2} between 1 and 4 given: {0}")
    @MethodSource("childrensDatesOfBirth")
    void shouldIncludeMetricsForNumberOfChildren(List<LocalDate> datesOfBirth, long childrenUnderOne, long childrenOneToFour) {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), datesOfBirth, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm1", String.valueOf(childrenUnderOne)),
                entry("cm2", String.valueOf(childrenOneToFour))
        );
    }

    static Stream<Arguments> childrensDatesOfBirth() {
        return Stream.of(
                Arguments.of(TWO_CHILDREN_UNDER_ONE, 2, 0),
                Arguments.of(ONE_CHILD_UNDER_ONE_AND_ONE_CHILD_BETWEEN_ONE_AND_FOUR, 1, 1),
                Arguments.of(TWO_CHILDREN_BETWEEN_ONE_AND_FOUR, 0, 2),
                Arguments.of(ONE_CHILD_FOUR_YEARS_OLD, 0, 0)
        );
    }


    @ParameterizedTest(name = "{1} weeks pregnant given {0} since conception")
    @CsvSource({
            "P10W, 10",
            "P20W1D, 20",
            "P36W6D, 36"
    })
    void shouldIncludeMetricsForPregnantClaimant(String timeSinceConception, Long weeksPregnant) {
        LocalDate expectedDeliveryDate = LocalDate.now().minus(Period.parse(timeSinceConception)).plusWeeks(40);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm3", "1"), // PREGNANCIES
                entry("cm9", String.valueOf(weeksPregnant))
        );
    }

    @Test
    void shouldNotIncludePregnancyWeeksForPregnancyInPast() {
        LocalDate expectedDeliveryDate = LocalDate.now().minusWeeks(40).plusDays(1);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
    }

    @Test
    void shouldNotIncludePregnancyWeeksForClaimantWithoutPregnancy() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
    }

    private void assertQueueTime(Map<String, String> reportProperties, LocalDateTime timestamp, int secondsSinceEvent) {
        // Queue time is number of milliseconds since now and the timestamp value. Therefore a timestamp one second ago should have a queue time >= 1000
        Long queueTime = Long.parseLong(reportProperties.get("qt"));
        Long maxPossibleQueueTime = ChronoUnit.MILLIS.between(timestamp, LocalDateTime.now());
        assertThat(queueTime).isBetween(TimeUnit.SECONDS.toMillis(secondsSinceEvent), maxPossibleQueueTime);
    }

    private void assertCommonProperties(Map<String, String> reportProperties, LocalDateTime timestamp, Claim claim, String eventCategory, String eventAction) {
        PostcodeData postcodeData = claim.getPostcodeData();
        assertThat(reportProperties).contains(
                entry("t", "event"),
                entry("v", "1"), // protocol version
                entry("tid", TRACKING_ID),
                entry("cid", claim.getId().toString()),
                entry("ec", eventCategory),
                entry("ea", eventAction),
                entry("cd1", "ONLINE"), // user type
                entry("cd3", CLAIMANT_CATEGORY.getDescription()), // claimant category
                entry("cd4", postcodeData.getAdminDistrict()), // local authority (admin district in postcodes.io).
                entry("cd5", postcodeData.getCodes().getAdminDistrict()), // local authority code
                entry("cd6", postcodeData.getCountry()),
                entry("cd7", postcodeData.getOutcode()),
                entry("cd8", postcodeData.getParliamentaryConstituency()),
                entry("cd9", postcodeData.getCcg()), // Clinical Commissioning Group
                entry("cd10", postcodeData.getCodes().getCcg()), // Clinical Commissioning Group code
                entry("cm1", "1"), // number of children under one
                entry("cm2", "0"), // number of children between one and four
                entry("cm3", "1"), // number of pregnancies
                entry("cm7", getExpectedClaimantAge(claim, timestamp)) // claimant age in years
        );
    }

    private ReportClaimMessageContext aReportClaimMessageContext(LocalDateTime timestamp,
                                                                 List<LocalDate> datesOfBirthOfChildren,
                                                                 LocalDate expectedDeliveryDate) {
        Claim claim = aClaimWithDueDateAndPostcodeData(expectedDeliveryDate);
        return ReportClaimMessageContext.builder()
                .claimAction(NEW)
                .claim(claim)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .timestamp(timestamp)
                .build();
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
                .claim(claim)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .timestamp(timestamp)
                .build();
    }

    private String getNumberOfWeeksPregnant(Claim claim, LocalDateTime timestamp) {
        LocalDate conception = claim.getClaimant().getExpectedDeliveryDate().minusWeeks(40);
        return String.valueOf(ChronoUnit.WEEKS.between(conception, timestamp));
    }

    private String getExpectedClaimantAge(Claim claim, LocalDateTime timestamp) {
        long claimantAge = Period.between(claim.getClaimant().getDateOfBirth(), timestamp.toLocalDate()).getYears();
        return String.valueOf(claimantAge);
    }
}
