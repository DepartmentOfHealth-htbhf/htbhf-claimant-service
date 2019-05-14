package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaimBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPaymentAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycleBuilder;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentTestDataFactory.aPaymentWithClaim;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class PaymentCycleRepositoryTest {

    @Autowired
    private PaymentCycleRepository paymentCycleRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    void afterEach() {
        paymentCycleRepository.deleteAll();
        paymentRepository.deleteAll();
        claimRepository.deleteAll();
    }

    @Test
    void shouldSaveNewPaymentCycle() {
        Claim claim = createAndSaveClaim();
        Payment payment = aPaymentWithClaim(claim);
        PaymentCycle paymentCycle = aPaymentCycleWithPaymentAndClaim(payment, claim);
        paymentCycle.addPayment(payment);

        PaymentCycle result = paymentCycleRepository.save(paymentCycle);

        assertThat(result).isEqualTo(paymentCycle);
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void shouldNotSaveAnInvalidPaymentCycle() {
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(null);

        Throwable thrown = catchThrowable(() -> paymentCycleRepository.save(paymentCycle));

        assertThat(thrown).hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldIdentifyClaimWithLatestCycleEndingToday() {
        LocalDate today = LocalDate.now();
        Claim claim = createAndSaveClaim();
        createAndSavePaymentCycleEnding(today, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo(claim.getId());
        assertThat(result.get(0).getCycleEndDate()).isEqualTo(today);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE", "PENDING_EXPIRY"})
    void shouldIdentifyPaymentCyclesForActiveClaims(ClaimStatus activeStatus) {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaimBuilder().claimStatus(activeStatus).build();
        claimRepository.save(claim);
        createAndSavePaymentCycleEnding(today, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo(claim.getId());
        assertThat(result.get(0).getCycleEndDate()).isEqualTo(today);
    }

    @ParameterizedTest
    @CsvSource({"REJECTED", "PENDING", "NEW", "EXPIRED", "ERROR"})
    void shouldNotIdentifyPaymentCyclesForInactiveOrNewClaims(ClaimStatus inactiveStatus) {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaimBuilder().claimStatus(inactiveStatus).build();
        claimRepository.save(claim);
        createAndSavePaymentCycleEnding(today, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldIdentifyClaimWithLatestCycleEndingYesterday() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Claim claim = createAndSaveClaim();
        PaymentCycle cycle = createAndSavePaymentCycleEnding(yesterday, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo(claim.getId());
        assertThat(result.get(0).getCycleId()).isEqualTo(cycle.getId());
        assertThat(result.get(0).getCycleEndDate()).isEqualTo(yesterday);
    }

    @Test
    void shouldNotIdentifyClaimWithLatestCycleEndingInFuture() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Claim claim = createAndSaveClaim();
        createAndSavePaymentCycleEnding(tomorrow, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAllClaimsWithPaymentCycleEndingTodayOrEarlier() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        createAndSavePaymentCycleEnding(today, createAndSaveClaim());
        createAndSavePaymentCycleEnding(yesterday, createAndSaveClaim());
        createAndSavePaymentCycleEnding(tomorrow, createAndSaveClaim());

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldIdentifyClaimOnceForLatestCycleOnly() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusDays(28);
        Claim claim = createAndSaveClaim();
        createAndSavePaymentCycleEnding(lastMonth, claim);
        createAndSavePaymentCycleEnding(today, claim);

        List<ClosingPaymentCycle> result = paymentCycleRepository.findActiveClaimsWithCycleEndingOnOrBefore(today);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClaimId()).isEqualTo(claim.getId());
        assertThat(result.get(0).getCycleEndDate()).isEqualTo(today);
    }

    @Test
    void shouldRetrieveNoPaymentCycles() {
        Iterable<PaymentCycle> allPaymentCycles = paymentCycleRepository.findAll();

        Iterator<PaymentCycle> paymentCycleIterator = allPaymentCycles.iterator();
        assertThat(paymentCycleIterator.hasNext()).isFalse();
    }

    private Claim createAndSaveClaim() {
        Claim claim = aValidClaim();
        claimRepository.save(claim);
        return claim;
    }

    private PaymentCycle createAndSavePaymentCycleEnding(LocalDate endDate, Claim claim) {
        PaymentCycle paymentCycle = aValidPaymentCycleBuilder()
                .claim(claim)
                .cycleStartDate(endDate.minusDays(28))
                .cycleEndDate(endDate)
                .build();
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

}
