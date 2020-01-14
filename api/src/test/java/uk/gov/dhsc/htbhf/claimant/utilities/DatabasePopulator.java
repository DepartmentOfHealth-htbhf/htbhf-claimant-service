package uk.gov.dhsc.htbhf.claimant.utilities;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleConfig;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import static java.util.UUID.randomUUID;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimantTestDataFactory.aClaimantWithNino;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchers;

/**
 * Creates Claims and PaymentCycle records in the database.
 * Designed to be run directly from an IDE, and ignored by gradle
 * To use this you should change the jdbc uri in the test application.yml to point to your local postgres instance (and specify username/password):
 *     spring:
 *         datasource:
 *         url: jdbc:postgresql://localhost/claimant
 *         username: claimant_admin
 *         password: claimant_admin
 *         driver-class-name: org.postgresql.Driver
 *         type: com.zaxxer.hikari.HikariDataSource
 *         hikari:
 *             connectionTimeout: 5000
 * Then un-comment the @Test annotation on generateClaimsAndPaymentCycles() and run the test.
 */
@Slf4j
@SpringBootTest
public class DatabasePopulator {

    // See also DatabasePopulatorTestContextConfiguration at the bottom of this class

    private static final String NINO_SUFFIX = "A";

    // The count of each type of claim to create
    private static final Map<ClaimStatus, Integer> CLAIMS_TO_CREATE = Map.of(
            ClaimStatus.ACTIVE, 400000,
            ClaimStatus.PENDING_EXPIRY, 100000,
            ClaimStatus.REJECTED, 400000
    );

    // The NINO prefix to use for each type of claim
    private static final Map<ClaimStatus, String> NINO_PREFIX = Map.of(
            ClaimStatus.ACTIVE, "EE",
            ClaimStatus.PENDING_EXPIRY, "PP",
            ClaimStatus.REJECTED, "RR"
    );

    // The eligibility status to save on the claim for each type of claim
    private static final Map<ClaimStatus, EligibilityStatus> CLAIM_ELIGIBILITY_STATUS = Map.of(
            ClaimStatus.ACTIVE, EligibilityStatus.ELIGIBLE,
            ClaimStatus.PENDING_EXPIRY, EligibilityStatus.ELIGIBLE,
            ClaimStatus.REJECTED, EligibilityStatus.NO_MATCH
    );

    // The eligibility status to save for payments - no entry for the claim status means no paymentCycle will be created
    private static final Map<ClaimStatus, EligibilityStatus> PAYMENT_ELIGIBILITY_STATUS = Map.of(
            ClaimStatus.ACTIVE, EligibilityStatus.ELIGIBLE,
            ClaimStatus.PENDING_EXPIRY, EligibilityStatus.INELIGIBLE
    );

    private final NumberFormat numberFormat = getNumberFormat();

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PaymentCycleRepository paymentCycleRepository;

    @Autowired
    private PaymentCycleConfig paymentCycleConfig;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ExecutorService fixedThreadPool;

    //@Test
    void generateClaimsAndPaymentCycles() {
        final AtomicInteger countCreated = new AtomicInteger();
        List<Long> durations = new ArrayList<>();

        CLAIMS_TO_CREATE.entrySet().stream().forEach(entry -> {
            Integer claims = entry.getValue();
            ClaimStatus status = entry.getKey();
            String ninoPrefix = NINO_PREFIX.get(status);
            NewClaimDetails details = NewClaimDetails.builder()
                    .status(status)
                    .ninoPrefix(ninoPrefix)
                    .eligibilityStatus(CLAIM_ELIGIBILITY_STATUS.get(status))
                    .paymentEligibility(PAYMENT_ELIGIBILITY_STATUS.get(status))
                    .cycleDuration(paymentCycleConfig.getEntitlementCalculationDurationInDays())
                    .build();
            Query query = entityManager.createNativeQuery("select count(*) from claimant where left(nino , 2) = '" + ninoPrefix + "'");
            int ninoStartIndex = ((Number) query.getSingleResult()).intValue() + 10001; // ensure we have a non-zero number in the first 2 digits
            log.error("Creating {} claims in status {}, starting from {}", claims, status, ninoStartIndex);
            List<Callable<Long>> jobs = new ArrayList<>(claims);
            for (int i = 0; i < claims; i++) {
                Integer ninoNumber = ninoStartIndex + i;
                Callable<Long> job = createNewClaimJob(details, ninoNumber, countCreated);
                jobs.add(job);
            }
            durations.addAll(invokeAllJobs(jobs));
        });

        logDurations(durations);
    }

    private Callable<Long> createNewClaimJob(NewClaimDetails details, Integer ninoNumber, AtomicInteger countCreated) {
        return () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            Claim claim = createClaim(details.getStatus(), details.getEligibilityStatus(), details.getNinoPrefix(), ninoNumber);
            if (details.getPaymentEligibility() != null) {
                createPaymentCycle(details.getCycleDuration(), details.getPaymentEligibility(), claim);
            }
            if (countCreated.incrementAndGet() % 1000 == 0) {
                log.error("Created {} claims so far", countCreated.get());
            }
            return stopWatch.getTime();
        };
    }

    private List<Long> invokeAllJobs(List<Callable<Long>> jobs) {
        List<Long> durations = new ArrayList<>();
        try {
            List<Future<Long>> futures = fixedThreadPool.invokeAll(jobs);
            for (Future<Long> future : futures) {
                durations.add(future.get());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return durations;
    }

    /**
     * This method logs a frequency map of the durations. The output looks like this:
     * Distribution of time to insert claim
     * 0-9ms    : 373793
     * 10-19ms  : 465927
     * 20-29ms  : 55405
     * 30-39ms  : 3303
     * 40-49ms  : 1087
     * 50-59ms  : 289
     * 60-69ms  : 87
     * 70-79ms  : 34
     * 80-89ms  : 37
     * 90-99ms  : 25
     * 100-109ms    : 7
     * Average time to insert claim: 12ms
     * @param durations the list of durations of the call to insert a claim
     */
    private void logDurations(List<Long> durations) {
        // aggregate the durations into buckets of 10ms
        Map<Long, Long> durationFrequencyMap = new TreeMap<>();
        int bucketSize = 10;
        for (Long duration : durations) {
            // identify the bucket this call belongs in - round down to nearest 10
            Long roundedDuration = (duration / bucketSize) * bucketSize;
            // increment the count of calls in this bucket
            durationFrequencyMap.merge(roundedDuration, 1L, Long::sum);
        }
        log.error("Distribution of time to insert claim");
        durationFrequencyMap.forEach((roundedDuration, count) -> log.error("{}-{}ms\t: {}", roundedDuration, roundedDuration + (bucketSize - 1), count));
        log.error("Average time to insert claim: {}ms", durations.stream().mapToLong(Long::longValue).sum() / durations.size());
    }

    private Claim createClaim(ClaimStatus status, EligibilityStatus eligibilityStatus, String ninoPrefix, int ninoNumber) {
        String nino = ninoPrefix + numberFormat.format(ninoNumber) + NINO_SUFFIX;
        Claim claim = aValidClaimBuilder()
                .claimStatus(status)
                .eligibilityStatus(eligibilityStatus)
                .dwpHouseholdIdentifier(randomUUID().toString())
                .hmrcHouseholdIdentifier(randomUUID().toString())
                .claimant(aClaimantWithNino(nino))
                .cardAccountId(randomUUID().toString())
                .build();
        claimRepository.save(claim);
        return claim;
    }

    private void createPaymentCycle(Integer cycleDuration, EligibilityStatus paymentEligibility, Claim claim) {
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithVouchers();
        LocalDate cycleStart = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(cycleDuration));
        PaymentCycle paymentCycle = PaymentCycle.builder()
                .claim(claim)
                .eligibilityStatus(paymentEligibility)
                .voucherEntitlement(voucherEntitlement)
                .totalVouchers(voucherEntitlement.getTotalVoucherEntitlement())
                .totalEntitlementAmountInPence(voucherEntitlement.getTotalVoucherValueInPence())
                .cycleStartDate(cycleStart)
                .cycleEndDate(cycleStart.plusDays(cycleDuration))
                .build();
        Payment payment = Payment.builder()
                .claim(claim)
                .cardAccountId(claim.getCardAccountId())
                .paymentAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentTimestamp(LocalDateTime.of(cycleStart, LocalTime.now()))
                .paymentReference(randomUUID().toString())
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();
        paymentCycle.addPayment(payment);
        paymentCycleRepository.save(paymentCycle);
    }

    private static NumberFormat getNumberFormat() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(0);
        format.setMinimumIntegerDigits(6);
        return format;
    }

    @Data
    @Builder
    private static class NewClaimDetails {
        private ClaimStatus status;
        private EligibilityStatus eligibilityStatus;
        private EligibilityStatus paymentEligibility;
        private String ninoPrefix;
        private Integer cycleDuration;
    }

    @TestConfiguration
    static class DatabasePopulatorTestContextConfiguration {
        @Bean("fixedThreadPool")
        public ExecutorService fixedThreadPool() {
            return Executors.newFixedThreadPool(6);
        }
    }

}
