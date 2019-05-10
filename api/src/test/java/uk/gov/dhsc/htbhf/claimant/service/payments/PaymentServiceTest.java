package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private MessageQueueDAO messageQueueDAO;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreateMakePaymentMessage() {
        PaymentCycle paymentCycle = aValidPaymentCycle();

        paymentService.createMakePaymentMessage(paymentCycle);

        ArgumentCaptor<MakePaymentMessagePayload> argumentCaptor = ArgumentCaptor.forClass(MakePaymentMessagePayload.class);
        verify(messageQueueDAO).sendMessage(argumentCaptor.capture(), eq(MessageType.MAKE_PAYMENT));
        assertMessagePayload(argumentCaptor.getValue(), paymentCycle);
    }

    private void assertMessagePayload(MakePaymentMessagePayload messagePayload, PaymentCycle paymentCycle) {
        assertThat(messagePayload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(messagePayload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(messagePayload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
    }
}
