package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleEntitlementCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForChildUnderFourNotificationWithNoPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectForClaimantWithAllVouchers;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidMakePaymentMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithVouchersForUnderOneOnly;

@ExtendWith(MockitoExtension.class)
class MakePaymentMessageProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private MessageQueueClient messageQueueClient;
    @Mock
    private ChildDateOfBirthCalculator childDateOfBirthCalculator;
    @Mock
    private PaymentCycleEntitlementCalculator paymentCycleEntitlementCalculator;

    @InjectMocks
    MakePaymentMessageProcessor processor;

    @Test
    void shouldProcessMessageAndSendPaymentEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessageContext messageContext = aValidMakePaymentMessageContext(paymentCycle, claim);
        given(messageContextLoader.loadMakePaymentContext(any())).willReturn(messageContext);
        given(childDateOfBirthCalculator.getNumberOfChildrenTurningFourAffectingNextPayment(any())).willReturn(0);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadMakePaymentContext(message);
        verify(paymentService).makePaymentForCycle(paymentCycle, claim.getCardAccountId());
        verify(childDateOfBirthCalculator).getNumberOfChildrenTurningFourAffectingNextPayment(paymentCycle);
        verifyPaymentEmailNotificationSent(paymentCycle, claim);
        verifyZeroInteractions(paymentCycleEntitlementCalculator);
    }

    @Test
    void shouldProcessMessageAndSendBothPaymentEmailAndChildTurnsFourEmail() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        Claim claim = paymentCycle.getClaim();
        MakePaymentMessageContext messageContext = aValidMakePaymentMessageContext(paymentCycle, claim);
        given(messageContextLoader.loadMakePaymentContext(any())).willReturn(messageContext);
        given(childDateOfBirthCalculator.getNumberOfChildrenTurningFourAffectingNextPayment(any())).willReturn(1);
        LocalDate startOfNextCycle = LocalDate.now().plusDays(28);
        //This entitlement specifically has no vouchers for 1-4 year olds.
        PaymentCycleVoucherEntitlement nextEntitlement = aPaymentCycleVoucherEntitlementWithVouchersForUnderOneOnly(startOfNextCycle);
        given(paymentCycleEntitlementCalculator.calculateEntitlement(any(), any(), any(), any())).willReturn(nextEntitlement);
        Message message = aValidMessageWithType(MAKE_PAYMENT);

        MessageStatus result = processor.processMessage(message);

        assertThat(result).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadMakePaymentContext(message);
        verify(paymentService).makePaymentForCycle(paymentCycle, claim.getCardAccountId());
        verify(childDateOfBirthCalculator).getNumberOfChildrenTurningFourAffectingNextPayment(paymentCycle);
        List<LocalDate> dateOfBirthOfChildren = List.of(
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusYears(3).minusMonths(6));
        Optional<LocalDate> expectedExpectedDeliveryDate = Optional.empty();
        verify(paymentCycleEntitlementCalculator).calculateEntitlement(
                expectedExpectedDeliveryDate,
                dateOfBirthOfChildren,
                startOfNextCycle,
                paymentCycle.getVoucherEntitlement()
        );
        verifyPaymentAndChildTurnsFourEmailNotificationsSent(paymentCycle, claim);
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
    }

    private void verifyPaymentEmailNotificationSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        assertThat(payloadCaptor.getAllValues()).hasSize(1);
        assertThat(payloadCaptor.getValue()).isInstanceOf(EmailMessagePayload.class);
        verifyPaymentEmail(paymentCycle, claim, payloadCaptor.getValue());
    }

    private void verifyPaymentAndChildTurnsFourEmailNotificationsSent(PaymentCycle paymentCycle, Claim claim) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient, times(2)).sendMessage(payloadCaptor.capture(), eq(MessageType.SEND_EMAIL));

        MessagePayload firstPayload = payloadCaptor.getAllValues().get(0);
        assertThat(firstPayload).isInstanceOf(EmailMessagePayload.class);
        verifyPaymentEmail(paymentCycle, claim, firstPayload);

        MessagePayload secondPayload = payloadCaptor.getAllValues().get(1);
        assertThat(secondPayload).isInstanceOf(EmailMessagePayload.class);
        verifyChildTurnsFourEmail(paymentCycle, claim, secondPayload);
    }

    private void verifyChildTurnsFourEmail(PaymentCycle paymentCycle, Claim claim, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CHILD_TURNS_FOUR);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertEmailPayloadCorrectForChildUnderFourNotificationWithNoPregnancyVouchers(payload.getEmailPersonalisation(),
                paymentCycle.getCycleEndDate().plusDays(1));
    }

    private void verifyPaymentEmail(PaymentCycle paymentCycle, Claim claim, MessagePayload messagePayload) {
        EmailMessagePayload payload = (EmailMessagePayload) messagePayload;
        assertThat(payload.getEmailType()).isEqualTo(EmailType.PAYMENT);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertEmailPayloadCorrectForClaimantWithAllVouchers(payload.getEmailPersonalisation(), paymentCycle.getCycleEndDate().plusDays(1));
    }

}
