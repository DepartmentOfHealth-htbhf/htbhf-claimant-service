package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.util.Iterator;
import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithPaymentAndClaim;
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
}
