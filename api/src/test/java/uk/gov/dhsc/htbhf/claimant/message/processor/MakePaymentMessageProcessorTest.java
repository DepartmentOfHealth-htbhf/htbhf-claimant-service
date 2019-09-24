package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.context.MakePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidMakePaymentMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class MakePaymentMessageProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private PaymentCycleEmailHandler paymentCycleEmailHandler;

    @InjectMocks
    MakePaymentMessageProcessor processor;

    @Test
    void shouldProcessMessageAndSendPaymentEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessageContext messageContext = aValidMakePaymentMessageContext(paymentCycle, claim);
        given(messageContextLoader.loadMakePaymentContext(any())).willReturn(messageContext);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadMakePaymentContext(message);
        verify(paymentService).makePaymentForCycle(paymentCycle, claim.getCardAccountId());
        verifyPaymentEmailNotificationSent(paymentCycle, claim);
        verify(paymentCycleEmailHandler).handleAdditionalEmails(paymentCycle);
    }

    @Test
    void shouldProcessFailedMessage() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessageContext messageContext = aValidMakePaymentMessageContext(paymentCycle, claim);
        given(messageContextLoader.loadMakePaymentContext(any())).willReturn(messageContext);
        Message message = mock(Message.class);
        FailureEvent failureEvent = mock(FailureEvent.class);

        processor.processFailedMessage(message, failureEvent);

        verify(messageContextLoader).loadMakePaymentContext(message);
        verify(paymentService).saveFailedPayment(paymentCycle, claim.getCardAccountId(), failureEvent);
        verifyZeroInteractions(paymentCycleEmailHandler);
    }

    private void verifyPaymentEmailNotificationSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        assertThat(payloadCaptor.getAllValues()).hasSize(1);
        assertThat(payloadCaptor.getValue()).isInstanceOf(EmailMessagePayload.class);
        verifyPaymentEmail(paymentCycle, claim, payloadCaptor.getValue());
    }

    private void verifyPaymentEmail(PaymentCycle paymentCycle, Claim claim, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), paymentCycle.getCycleEndDate().plusDays(1));
    }

}
