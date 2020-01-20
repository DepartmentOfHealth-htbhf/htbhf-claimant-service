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
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.CompletePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.PaymentType;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCalculation;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentCycleService;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentResult;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.INITIAL_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.SCHEDULED_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidRequestPaymentMessageContextForFirstPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidRequestPaymentMessageContextForScheduledPayment;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aFullPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aNoPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentResultTestDataFactory.aValidPaymentResult;

@ExtendWith(MockitoExtension.class)
class RequestPaymentMessageProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private PaymentCycleService paymentCycleService;
    @Mock
    private EventAuditor eventAuditor;

    @InjectMocks
    private RequestPaymentMessageProcessor processor;

    @Test
    void shouldProcessMessageForScheduledPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        RequestPaymentMessageContext messageContext = aValidRequestPaymentMessageContextForScheduledPayment(paymentCycle, claim);
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        PaymentResult paymentResult = aValidPaymentResult();
        given(messageContextLoader.loadRequestPaymentMessageContext(any())).willReturn(messageContext);
        given(paymentService.calculatePaymentAmount(any())).willReturn(paymentCalculation);
        given(paymentService.makePayment(any(), anyInt(), any())).willReturn(paymentResult);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadRequestPaymentMessageContext(message);
        verify(paymentService).calculatePaymentAmount(paymentCycle);
        verify(paymentService).makePayment(paymentCycle, paymentCalculation.getPaymentAmount(), SCHEDULED_PAYMENT);
        verifyNoInteractions(paymentCycleService);
        assertCompletePayloadMessageSent(paymentCycle, paymentResult, paymentCalculation, PaymentType.REGULAR_PAYMENT);
    }

    @Test
    void shouldProcessMessageForFirstPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        RequestPaymentMessageContext messageContext = aValidRequestPaymentMessageContextForFirstPayment(paymentCycle, claim);
        PaymentResult paymentResult = aValidPaymentResult();
        given(messageContextLoader.loadRequestPaymentMessageContext(any())).willReturn(messageContext);
        given(paymentService.makePayment(any(), anyInt(), any())).willReturn(paymentResult);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadRequestPaymentMessageContext(message);
        verify(paymentService).makePayment(paymentCycle, paymentCycle.getTotalEntitlementAmountInPence(), INITIAL_PAYMENT);
        PaymentCalculation expectedCalculation = PaymentCalculation.builder()
                .paymentAmount(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentCycleStatus(PaymentCycleStatus.FULL_PAYMENT_MADE)
                .build();
        assertCompletePayloadMessageSent(paymentCycle, paymentResult, expectedCalculation, PaymentType.FIRST_PAYMENT);
    }

    @Test
    void shouldNotMakeAPaymentWhenBalanceIsTooHigh() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        RequestPaymentMessageContext messageContext = aValidRequestPaymentMessageContextForScheduledPayment(paymentCycle, claim);
        PaymentCalculation paymentCalculation = aNoPaymentCalculation();
        given(messageContextLoader.loadRequestPaymentMessageContext(any())).willReturn(messageContext);
        given(paymentService.calculatePaymentAmount(any())).willReturn(paymentCalculation);
        Message message = aValidMessageWithType(REQUEST_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadRequestPaymentMessageContext(message);
        verify(paymentService).calculatePaymentAmount(paymentCycle);
        verify(paymentCycleService).updatePaymentCycleFromCalculation(paymentCycle, paymentCalculation);
        verify(eventAuditor).auditBalanceTooHighForPayment(paymentCycle);
        verifyNoMoreInteractions(paymentService);
        verify(paymentCycleService).updatePaymentCycleFromCalculation(paymentCycle, paymentCalculation);
    }

    private void assertCompletePayloadMessageSent(PaymentCycle paymentCycle,
                                                  PaymentResult paymentResult,
                                                  PaymentCalculation paymentCalculation,
                                                  PaymentType paymentType) {
        ArgumentCaptor<CompletePaymentMessagePayload> argumentCaptor = ArgumentCaptor.forClass(CompletePaymentMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(COMPLETE_PAYMENT));
        CompletePaymentMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(payload.getPaymentCalculation()).isEqualToIgnoringGivenFields(paymentCalculation, "balanceTimestamp");
        assertThat(payload.getPaymentCalculation().getBalanceTimestamp()).isNotNull();
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(payload.getPaymentResult()).isEqualTo(paymentResult);
        assertThat(payload.getPaymentType()).isEqualTo(paymentType);
    }
}
