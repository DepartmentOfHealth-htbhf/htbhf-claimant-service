package uk.gov.dhsc.htbhf.claimant.reporting.payload;

import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.PostcodeData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

abstract class ReportPropertiesFactoryTest {

    protected static final String TRACKING_ID = "tracking-id";
    protected static final ClaimantCategory CLAIMANT_CATEGORY = ClaimantCategory.PREGNANT_WITH_CHILDREN;

    protected void assertQueueTime(Map<String, String> reportProperties, LocalDateTime timestamp, int secondsSinceEvent) {
        // Queue time is number of milliseconds since now and the timestamp value. Therefore a timestamp one second ago should have a queue time >= 1000
        Long queueTime = Long.parseLong(reportProperties.get("qt"));
        Long maxPossibleQueueTime = ChronoUnit.MILLIS.between(timestamp, LocalDateTime.now());
        assertThat(queueTime).isBetween(TimeUnit.SECONDS.toMillis(secondsSinceEvent), maxPossibleQueueTime);
    }

    protected void assertCommonProperties(Map<String, String> reportProperties,
                                          LocalDateTime timestamp,
                                          Claim claim,
                                          String eventCategory,
                                          String eventAction) {
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
                entry("cm1", "0"), // number of children under one
                entry("cm2", "1"), // number of children between one and four
                entry("cm3", "1"), // number of pregnancies
                entry("cm7", getExpectedClaimantAge(claim, timestamp)) // claimant age in years
        );
    }

    protected String getNumberOfWeeksPregnant(Claim claim, LocalDateTime timestamp) {
        LocalDate conception = claim.getClaimant().getExpectedDeliveryDate().minusWeeks(40);
        return String.valueOf(ChronoUnit.WEEKS.between(conception, timestamp));
    }

    protected String getExpectedClaimantAge(Claim claim, LocalDateTime timestamp) {
        long claimantAge = Period.between(claim.getClaimant().getDateOfBirth(), timestamp.toLocalDate()).getYears();
        return String.valueOf(claimantAge);
    }
}
