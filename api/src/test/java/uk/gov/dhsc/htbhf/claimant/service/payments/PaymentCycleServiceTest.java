package uk.gov.dhsc.htbhf.claimant.service.payments;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class PaymentCycleServiceTest {

    @Autowired
    private PaymentCycleService paymentCycleService;

    @MockBean
    private PaymentCycleRepository paymentCycleRepository;

    @Test
    void shouldCreateNewPaymentCycle() {
        LocalDate today = LocalDate.now();
        Claim claim = aValidClaim();

        PaymentCycle result = paymentCycleService.createAndSavePaymentCycle(claim, today);

        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(argumentCaptor.capture());
        PaymentCycle paymentCycle = argumentCaptor.getValue();
        assertThat(paymentCycle.getClaim()).isEqualTo(claim);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(today);
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(today.plusDays(28));
        assertThat(result).isEqualTo(paymentCycle);
    }
}
