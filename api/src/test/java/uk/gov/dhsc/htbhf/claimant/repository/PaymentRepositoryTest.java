package uk.gov.dhsc.htbhf.claimant.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;

import java.util.Iterator;
import javax.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentTestDataFactory.aPaymentWithCardAccountId;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentTestDataFactory.aValidPayment;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class PaymentRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @AfterEach
    public void tearDown() {
        paymentRepository.deleteAll();
        claimRepository.deleteAll();
    }

    @Test
    void shouldSavePayment() {
        Payment payment = aValidPayment();
        claimRepository.save(payment.getClaim());

        Payment result = paymentRepository.save(payment);

        assertThat(result).isEqualTo(payment);
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void shouldNotSaveAnInvalidPayment() {
        Payment payment = aPaymentWithCardAccountId(null);

        Throwable thrown = catchThrowable(() -> paymentRepository.save(payment));

        assertThat(thrown).hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRetrieveNoPayments() {
        Iterable<Payment> allPayments = paymentRepository.findAll();

        Iterator<Payment> paymentIterator = allPayments.iterator();
        assertThat(paymentIterator.hasNext()).isFalse();
    }
}
