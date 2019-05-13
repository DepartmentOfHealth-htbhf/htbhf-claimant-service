package uk.gov.dhsc.htbhf.claimant.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithClaimStatusTimestamp;

@SpringBootTest
@AutoConfigureEmbeddedDatabase
class PaymentCycleServiceTest {

    @Autowired
    private PaymentCycleService paymentCycleService;

    @MockBean
    private PaymentCycleRepository paymentCycleRepository;

    @Test
    void shouldCreateNewPaymentCycle() {
        LocalDateTime today = LocalDateTime.now();
        Claim claim = aClaimWithClaimStatusTimestamp(today);

        paymentCycleService.createNewPaymentCycle(claim);

        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleRepository).save(argumentCaptor.capture());
        PaymentCycle paymentCycle = argumentCaptor.getValue();
        assertThat(paymentCycle.getClaim()).isEqualTo(claim);
        assertThat(paymentCycle.getCycleStartDate()).isEqualTo(today.toLocalDate());
        assertThat(paymentCycle.getCycleEndDate()).isEqualTo(today.toLocalDate().plusDays(28));
    }
}
