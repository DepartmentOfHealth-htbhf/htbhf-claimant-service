package uk.gov.dhsc.htbhf.claimant.utilities;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
 * To use this you should change the jdbc uri in the test application.yml to point to your local postgres instance (and specify username/password).
 */
@SpringBootTest
@Slf4j
@Disabled
public class DatabasePopulator {

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

    @Test
    void generateClaimsAndPaymentCycles() {
        final AtomicInteger countCreated = new AtomicInteger();

        Integer cycleDuration = paymentCycleConfig.getEntitlementCalculationDurationInDays();

        CLAIMS_TO_CREATE.entrySet().stream().forEach(entry -> {
            Integer claims = entry.getValue();
            ClaimStatus status = entry.getKey();
            EligibilityStatus eligibilityStatus = CLAIM_ELIGIBILITY_STATUS.get(status);
            EligibilityStatus paymentEligibility = PAYMENT_ELIGIBILITY_STATUS.get(status);
            String ninoPrefix = NINO_PREFIX.get(status);
            Query query = entityManager.createNativeQuery("select count(*) from claimant where left(nino , 2) = '" + ninoPrefix + "'");
            int ninoStartIndex = ((Number)query.getSingleResult()).intValue() + 1;
            log.error("Creating {} claims in status {}, starting from {}", claims, status, ninoStartIndex);
            for (int i = 0; i < claims; i++) {
                Claim claim = createClaim(status, eligibilityStatus, ninoPrefix, ninoStartIndex + i);
                if (paymentEligibility != null) {
                    createPaymentCycle(cycleDuration, paymentEligibility, claim);
                }
                if (countCreated.incrementAndGet() % 100 == 0) {
                    log.error("Created {} claims so far", countCreated.get());
                }

            }
        });
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
}
