package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;

import java.util.Iterator;
import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class PaymentCycleRepositoryTest {

    @Autowired
    private PaymentCycleRepository paymentCycleRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @AfterEach
    void afterEach() {
        paymentCycleRepository.deleteAll();
        claimRepository.deleteAll();
    }

    @Test
    void shouldSaveNewPaymentCycle() {
        // TODO add payment
        PaymentCycle paymentCycle = aValidPaymentCycle();
        claimRepository.save(paymentCycle.getClaim());

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
}