package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.communications.PaymentCycleNotificationHandler;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.CompletePaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidCompletePaymentMessageContextForFirstPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidCompletePaymentMessageContextForRestartedPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidCompletePaymentMessageContextForScheduledPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;

@ExtendWith(MockitoExtension.class)
class CompletePaymentMessageProcessorTest {

    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentCycleNotificationHandler paymentCycleNotificationHandler;

    @InjectMocks
    private CompletePaymentMessageProcessor processor;

    @Test
    void shouldProcessMessageAndSendRegularPaymentEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        CompletePaymentMessageContext messageContext = aValidCompletePaymentMessageContextForScheduledPayment(paymentCycle, claim);
        given(messageContextLoader.loadCompletePaymentMessageContext(any())).willReturn(messageContext);
        Message message = aValidMessageWithType(COMPLETE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadCompletePaymentMessageContext(message);
        verify(paymentService).completePayment(paymentCycle, messageContext.getPaymentCalculation(), messageContext.getPaymentResult());
        verify(paymentCycleNotificationHandler).sendNotificationEmailsForRegularPayment(paymentCycle);
    }

    @Test
    void shouldProcessMessageAndSendRestartedPaymentEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        CompletePaymentMessageContext messageContext = aValidCompletePaymentMessageContextForRestartedPayment(paymentCycle, claim);
        given(messageContextLoader.loadCompletePaymentMessageContext(any())).willReturn(messageContext);
        Message message = aValidMessageWithType(COMPLETE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadCompletePaymentMessageContext(message);
        verify(paymentService).completePayment(paymentCycle, messageContext.getPaymentCalculation(), messageContext.getPaymentResult());
        verify(paymentCycleNotificationHandler).sendNotificationEmailsForRestartedPayment(paymentCycle);
    }

    @Test
    void shouldProcessMessageAndSendNoEmailsForInitialPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        CompletePaymentMessageContext messageContext = aValidCompletePaymentMessageContextForFirstPayment(paymentCycle, claim);
        given(messageContextLoader.loadCompletePaymentMessageContext(any())).willReturn(messageContext);
        Message message = aValidMessageWithType(COMPLETE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadCompletePaymentMessageContext(message);
        verify(paymentService).completePayment(paymentCycle, messageContext.getPaymentCalculation(), messageContext.getPaymentResult());
        verifyNoInteractions(paymentCycleNotificationHandler);
    }
}
