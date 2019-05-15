package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.PayloadMapper;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class MakePaymentMessageProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private PayloadMapper payloadMapper;
    @Mock
    private MessageContextLoader messageContextLoader;

    @InjectMocks
    MakePaymentMessageProcessor processor;

    @Test
    void shouldProcessMessage() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessagePayload messagePayload = aValidMessagePayload(paymentCycle, claim);
        MakePaymentMessageContext messageContext = aValidMessageContext(paymentCycle, claim);
        given(payloadMapper.getPayload(any(), any())).willReturn(messagePayload);
        given(messageContextLoader.loadContext(any(MakePaymentMessagePayload.class))).willReturn(messageContext);
        Message message = aValidMessage();


        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(payloadMapper).getPayload(message, MakePaymentMessagePayload.class);
        verify(messageContextLoader).loadContext(messagePayload);
        verify(paymentService).makePayment(paymentCycle, claim.getCardAccountId());
        verify(messageRepository).delete(message);
    }

    private MakePaymentMessageContext aValidMessageContext(PaymentCycle paymentCycle, Claim claim) {
        return MakePaymentMessageContext.builder()
                .paymentCycle(paymentCycle)
                .claim(claim)
                .cardAccountId(claim.getCardAccountId())
                .build();
    }

    private MakePaymentMessagePayload aValidMessagePayload(PaymentCycle paymentCycle, Claim claim) {
        return MakePaymentMessagePayload.builder()
                .paymentCycleId(paymentCycle.getId())
                .claimId(claim.getId())
                .cardAccountId(claim.getCardAccountId())
                .build();
    }
}