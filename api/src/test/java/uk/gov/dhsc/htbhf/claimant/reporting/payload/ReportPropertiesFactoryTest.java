package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.context.ReportClaimMessageContext;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;
import uk.gov.dhsc.htbhf.claimant.reporting.ClaimantCategoryCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.reporting.ClaimAction.NEW;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithExpectedDeliveryDate;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PostcodeDataTestDataFactory.aPostcodeDataObjectForPostcode;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.EXPECTED_DELIVERY_DATE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_POSTCODE;

@ExtendWith(MockitoExtension.class)
class ReportPropertiesFactoryTest {

    private static final ClaimantCategory CLAIMANT_CATEGORY = ClaimantCategory.PREGNANT_WITH_CHILDREN;

    @Mock
    private ClaimantCategoryCalculator claimantCategoryCalculator;

    @InjectMocks
    private ReportPropertiesFactory reportPropertiesFactory;

    @Test
    void shouldCreateReportPropertiesForNewClaim() {
        LocalDateTime timestamp = LocalDateTime.now().minusSeconds(1);
        List<LocalDate> datesOfBirthOfChildren = singletonList(LocalDate.now().minusMonths(11));
        ReportClaimMessageContext context = aReportClaimMessageContext(timestamp, datesOfBirthOfChildren, EXPECTED_DELIVERY_DATE);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);


        // Queue time is number of milliseconds since now and the timestamp value. Therefore a timestamp one second ago should have a queue time >= 1000
        Long queueTime = Long.parseLong(reportProperties.get("qt"));
        assertThat(queueTime).isGreaterThanOrEqualTo(1000);
        long maxQueueTime = ChronoUnit.MILLIS.between(timestamp, LocalDateTime.now());
        assertThat(queueTime).isLessThanOrEqualTo(maxQueueTime);

        Claim claim = context.getClaim();
        PostcodeData postcodeData = claim.getPostcodeData();
        assertThat(reportProperties).contains(
                entry("t", "event"),
                entry("ec", "CLAIM"),
                entry("ea", "NEW"),
                entry("ev", "0"),
                entry("cid", claim.getId().toString()),
                entry("cd1", "ONLINE"), // user type
                entry("cd3", CLAIMANT_CATEGORY.getDescription()), // claimant category
                entry("cd4", postcodeData.getAdminDistrict()), // local authority (admin district in postcodes.io).
                entry("cd5", postcodeData.getCodes().getAdminDistrict()), // local authority code
                entry("cd6", postcodeData.getCountry()),
                entry("cd7", postcodeData.getOutcode()),
                entry("cd8", postcodeData.getParliamentaryConstituency()),
                entry("cd9", postcodeData.getCcg()), // Clinical Commissioning Group
                entry("cd10", postcodeData.getCodes().getCcg())); // Clinical Commissioning Group code
        long expectedClaimantAge = Period.between(claim.getClaimant().getDateOfBirth(), timestamp.toLocalDate()).getYears();
        assertThat(reportProperties.get("cm7")).isEqualTo(String.valueOf(expectedClaimantAge));
        assertThat(reportProperties).doesNotContainKeys("cm4", "cm5", "cm6", "cm8"); // payment-only custom metrics
        verify(claimantCategoryCalculator).determineClaimantCategory(claim.getClaimant(), datesOfBirthOfChildren, timestamp.toLocalDate());
    }

    @Test
    void shouldImposeMaximumValueForQueueTime() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now().minusHours(5), emptyList(), null);
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
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), emptyList(), null);
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
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), datesOfBirth, null);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm1", String.valueOf(childrenUnderOne)),
                entry("cm2", String.valueOf(childrenOneToFour))
        );
    }

    static Stream<Arguments> childrensDatesOfBirth() {
        return Stream.of(
                Arguments.of(List.of(LocalDate.now().minusDays(1), LocalDate.now().minusMonths(11)), 2, 0),
                Arguments.of(List.of(LocalDate.now().minusMonths(11), LocalDate.now().minusYears(2)), 1, 1),
                Arguments.of(List.of(LocalDate.now().minusMonths(11), LocalDate.now().minusYears(2)), 1, 1),
                Arguments.of(List.of(LocalDate.now().minusYears(2), LocalDate.now().minusYears(3)), 0, 2),
                Arguments.of(List.of(LocalDate.now().minusYears(4)), 0, 0)
        );
    }


    @ParameterizedTest(name = "{1} weeks pregnant given {0} since conception")
    @CsvSource({
            "P10W, 10",
            "P20W1D, 20",
            "P36W6D, 36"
    })
    void shouldIncludeMetricsForPregnantClaimant(String timeSinceConception, Long weeksPregnant) {
        LocalDate expectedDeliveryDate = LocalDate.now().minus(Period.parse(timeSinceConception)).plusMonths(9);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), emptyList(), expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(
                entry("cm3", "1"), // PREGNANCIES
                entry("cm9", String.valueOf(weeksPregnant))
        );
    }

    @Test
    void shouldNotIncludePregnancyWeeksForPregnancyInPast() {
        LocalDate expectedDeliveryDate = LocalDate.now().minusMonths(9).plusDays(1);
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), emptyList(), expectedDeliveryDate);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
    }

    @Test
    void shouldNotIncludePregnancyWeeksForClaimantWithoutPregnancy() {
        ReportClaimMessageContext context = aReportClaimMessageContext(LocalDateTime.now(), emptyList(), null);
        given(claimantCategoryCalculator.determineClaimantCategory(any(), any(), any())).willReturn(CLAIMANT_CATEGORY);

        Map<String, String> reportProperties = reportPropertiesFactory.createReportPropertiesForClaimEvent(context);

        assertThat(reportProperties).contains(entry("cm3", "0"));
        assertThat(reportProperties).doesNotContainKey("cm9");
    }

    private ReportClaimMessageContext aReportClaimMessageContext(LocalDateTime timestamp, List<LocalDate> datesOfBirthOfChildren,
                                                                 LocalDate expectedDeliveryDate) {
        Claimant claimant = aClaimantWithExpectedDeliveryDate(expectedDeliveryDate);
        Claim claim = aValidClaimBuilder()
                .claimant(claimant)
                .postcodeData(aPostcodeDataObjectForPostcode(VALID_POSTCODE))
                .build();
        return ReportClaimMessageContext.builder()
                .claimAction(NEW)
                .claim(claim)
                .datesOfBirthOfChildren(datesOfBirthOfChildren)
                .timestamp(timestamp)
                .build();
    }
}
