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
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.TestConstants.*;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.FIRST_NAME;
import static uk.gov.dhsc.htbhf.claimant.model.UpdatableClaimantField.LAST_NAME;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ReportClaimMessageContextTestDataFactory.aReportClaimMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ReportClaimMessageContextTestDataFactory.aReportClaimMessageContextForAnUpdatedClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ReportClaimMessageContextTestDataFactory.aReportClaimMessageContextWithoutDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.NOT_PREGNANT;

@ExtendWith(MockitoExtension.class)
class ReportClaimPropertiesFactoryTest extends ReportPropertiesFactoryTest {

    @Mock
    private ClaimantCategoryCalculator claimantCategoryCalculator;

    private ReportClaimPropertiesFactory reportClaimPropertiesFactory;

    @BeforeEach
    void init() {
        reportClaimPropertiesFactory = new ReportClaimPropertiesFactory(TRACKING_ID, claimantCategoryCalculator);
    }

    @Test
    void shouldCreateReportPropertiesForClaim() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportClaimMessageContext context = aReportClaimMessageContext(timestamp, datesOfBirthOfChildren, EXPECTED_DELIVERY_DATE_IN_TWO_MONTHS);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertQueueTime(reportProperties, timestamp, secondsSinceEvent);
        Claim claim = context.getClaim();
        assertCommonProperties(reportProperties, timestamp, claim, "CLAIM", "NEW");
        assertThat(reportProperties).contains(
                entry("ev", "0"), // event value (set to 0 as it's not used for claim events)
                entry("cm9", getNumberOfWeeksPregnant(claim, timestamp)), // weeks pregnant
                entry("cd11", getNumberOfWeeksPregnant(claim, timestamp)) // weeks pregnant is recorded as both a metric and a dimension
        );
        assertThat(reportProperties).doesNotContainKeys("cm4", "cm5", "cm6", "cm8"); // payment-only custom metrics
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate(), null);
    }

    @Test
    void shouldCreateReportPropertiesForClaimWithoutIdentityAndEligibilityResponse() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportClaimMessageContext context = aReportClaimMessageContextWithoutDecision(timestamp,
                datesOfBirthOfChildren);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        Claim claim = context.getClaim();
        assertThat(reportProperties).contains(
                entry("cd12", "NOT_SET"), // qualifying benefit
                entry("cm1", "1"), // number of children under one
                entry("cm2", "0") // number of children between one and four
        );
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate(), null);
    }

    @Test
    void shouldCreateReportPropertiesForClaimWithUpdatedClaimantFields() {
        int secondsSinceEvent = 1;
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(secondsSinceEvent);
        ReportClaimMessageContext context = aReportClaimMessageContextForAnUpdatedClaim(timestamp, NO_CHILDREN, NOT_PREGNANT, List.of(FIRST_NAME, LAST_NAME));
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("el", "firstName, lastName"),
                entry("ea", "UPDATED")
        );
    }

    @Test
    void shouldImposeMaximumValueForQueueTime() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now().minusHours(5), NO_CHILDREN, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        // queue times longer than 4 hours may cause the event to not be registered:
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
        Long queueTime = Long.parseLong(reportProperties.get("qt"));
        long maxQueueTime = 1000 * 60 * 60 * 4;
        assertThat(queueTime).isLessThanOrEqualTo(maxQueueTime);
    }

    @ParameterizedTest(name = "{1} children under 1 and {2} between 1 and 4 given: {0}")
    @MethodSource("childrensDatesOfBirth")
    void shouldIncludeMetricsForNumberOfChildren(List<LocalDate> datesOfBirth, long childrenUnderOne, long childrenOneToFour) {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), datesOfBirth, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm1", String.valueOf(childrenUnderOne)),
                entry("cm2", String.valueOf(childrenOneToFour))
        );
    }

    static Stream<Arguments> childrensDatesOfBirth() {
        return Stream.of(
                Arguments.of(NO_CHILDREN, 0, 0),
                Arguments.of(null, 0, 0),
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
    void shouldIncludeReportPropertiesForPregnantClaimant(String timeSinceConception, Long weeksPregnant) {
        LocalDate expectedDeliveryDate = LocalDate.now().minus(Period.parse(timeSinceConception)).plusWeeks(40);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm3", "1"), // PREGNANCIES
                entry("cm9", String.valueOf(weeksPregnant)),
                entry("cd11", String.valueOf(weeksPregnant))
        );
    }

    @Test
    void shouldNotIncludePregnancyWeeksForPregnancyInPast() {
        LocalDate expectedDeliveryDate = LocalDate.now().minusWeeks(40).plusDays(1);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
        assertThat(reportProperties).doesNotContainKey("cd11");
    }

    @Test
    void shouldNotIncludePregnancyWeeksForClaimantWithoutPregnancy() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), NO_CHILDREN, NOT_PREGNANT);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportClaimPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
        assertThat(reportProperties).doesNotContainKey("cd11");
    }
}