package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.MakePaymentEvent;
import uk.gov.dhsc.htbhf.logging.event.CommonEventType;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.BALANCE_TOO_HIGH_FOR_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentCycleStatus.FULL_PAYMENT_MADE;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventMetadataKey.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.FailureEventTestDataFactory.aFailureEventWithEvent;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aFullPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aNoPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.AVAILABLE_BALANCE_IN_PENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.EXCEPTION_DETAIL_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILED_EVENT_KEY;
import static uk.gov.dhsc.htbhf.logging.event.FailureEvent.FAILURE_DESCRIPTION_KEY;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String CARD_PROVIDER_PAYMENT_REFERENCE = "cardProviderPaymentReference";

    @Mock
    private MessageQueueDAO messageQueueDAO;

    @Mock
    private CardClient cardClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventAuditor eventAuditor;

    @Mock
    private PaymentCycleService paymentCycleService;

    @Mock
    private PaymentCalculator paymentCalculator;

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

    @Test
    void shouldMakeFirstPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        Payment paymentResult = paymentService.makeFirstPayment(paymentCycle, CARD_ACCOUNT_ID);

        assertSuccessfulPayment(paymentResult, paymentCycle, paymentCycle.getTotalEntitlementAmountInPence());
        verify(paymentRepository).save(paymentResult);
        verify(eventAuditor).auditMakePayment(paymentCycle, paymentResult, depositFundsResponse);
        verifyDepositFundsRequestCorrectWithSpecificReference(paymentCycle.getTotalEntitlementAmountInPence(), paymentResult.getId().toString());
        verifyPaymentCycleStatusSaved(paymentCycle, FULL_PAYMENT_MADE);
    }

    @Test
    void shouldAuditFailedPaymentWhenFirstPaymentFails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        RuntimeException testException = new RuntimeException("test exception");
        given(cardClient.depositFundsToCard(any(), any())).willThrow(testException);

        EventFailedException exception = catchThrowableOfType(() -> paymentService.makeFirstPayment(paymentCycle, CARD_ACCOUNT_ID), EventFailedException.class);

        String expectedFailureMessage = String.format("First payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                CARD_ACCOUNT_ID, paymentCycle.getClaim().getId(), paymentCycle.getId(), "test exception");
        verifyEventFailExceptionAndEventAreCorrect(paymentCycle, testException, exception,
                expectedFailureMessage, paymentCycle.getTotalEntitlementAmountInPence());

        verifyZeroInteractions(paymentRepository, paymentCycleService, eventAuditor);
        verifyDepositFundsRequestCorrectWithAnyReference(paymentCycle.getTotalEntitlementAmountInPence());
    }

    @Test
    void shouldMakePayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentCalculation);
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        Payment paymentResult = paymentService.makePayment(paymentCycle, CARD_ACCOUNT_ID);

        assertSuccessfulPayment(paymentResult, paymentCycle, paymentCalculation.getPaymentAmount());
        verify(paymentRepository).save(paymentResult);
        verify(eventAuditor).auditMakePayment(paymentCycle, paymentResult, depositFundsResponse);
        verify(cardClient).getBalance(CARD_ACCOUNT_ID);
        verifyPaymentCycleStatusAndBalanceUpdated(paymentCycle, balanceResponse.getAvailableBalanceInPence(), FULL_PAYMENT_MADE);
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        verifyDepositFundsRequestCorrectWithSpecificReference(paymentCalculation.getPaymentAmount(), paymentResult.getId().toString());
    }

    @Test
    void shouldAuditFailedPaymentWhenPaymentFails() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentCalculation);
        RuntimeException testException = new RuntimeException("test exception");
        given(cardClient.depositFundsToCard(any(), any())).willThrow(testException);

        EventFailedException exception = catchThrowableOfType(() -> paymentService.makePayment(paymentCycle, CARD_ACCOUNT_ID), EventFailedException.class);

        String expectedFailureMessage = String.format("Payment failed for cardAccountId %s, claim %s, paymentCycle %s, exception is: %s",
                CARD_ACCOUNT_ID, paymentCycle.getClaim().getId(), paymentCycle.getId(), "test exception");
        verifyEventFailExceptionAndEventAreCorrect(paymentCycle, testException, exception, expectedFailureMessage, paymentCalculation.getPaymentAmount());

        verify(cardClient).getBalance(CARD_ACCOUNT_ID);
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        //This may be called, but the transaction will be rolled back, so it wont be actually be updated
        verifyPaymentCycleStatusAndBalanceUpdated(paymentCycle, balanceResponse.getAvailableBalanceInPence(), FULL_PAYMENT_MADE);
        verifyZeroInteractions(paymentRepository, eventAuditor);
        verifyDepositFundsRequestCorrectWithAnyReference(paymentCalculation.getPaymentAmount());
    }

    @Test
    void shouldMakeNoPaymentWhenBalanceIsTooHigh() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(aNoPaymentCalculation());

        Payment paymentResult = paymentService.makePayment(paymentCycle, CARD_ACCOUNT_ID);

        assertThat(paymentResult).isNull();
        verify(cardClient).getBalance(CARD_ACCOUNT_ID);
        verifyPaymentCycleStatusAndBalanceUpdated(paymentCycle, balanceResponse.getAvailableBalanceInPence(), BALANCE_TOO_HIGH_FOR_PAYMENT);
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        verify(eventAuditor).auditBalanceTooHighForPayment(paymentCycle);
        verifyZeroInteractions(paymentRepository);
        verifyNoMoreInteractions(cardClient, eventAuditor);
    }

    @Test
    void shouldSaveFailedPayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        int amountToPay = 1240;
        String paymentReference = "paymentRef1";
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentAmountInPence(amountToPay)
                .paymentId(UUID.randomUUID())
                .reference(paymentReference)
                .build();

        paymentService.saveFailedPayment(paymentCycle, CARD_ACCOUNT_ID, aFailureEventWithEvent(event));

        verifyFailedPaymentSavedWithAllData(paymentCycle, amountToPay, paymentReference);
    }

    @Test
    void shouldSaveFailedPaymentWithNoAmountOrReferenceOnEvent() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .entitlementAmountInPence(paymentCycle.getTotalEntitlementAmountInPence())
                .paymentAmountInPence(null)
                .paymentId(null)
                .reference(null)
                .build();

        paymentService.saveFailedPayment(paymentCycle, CARD_ACCOUNT_ID, aFailureEventWithEvent(event));

        verifyFailedPaymentSavedWithNoOptionalData(paymentCycle);
    }

    private void verifyFailedPaymentSavedWithNoOptionalData(PaymentCycle paymentCycle) {
        Payment actualPayment = verifyFailedPaymentSavedCorrectly(paymentCycle);
        assertThat(actualPayment.getPaymentAmountInPence()).isNull();
        assertThat(actualPayment.getPaymentReference()).isNull();
    }

    private void verifyFailedPaymentSavedWithAllData(PaymentCycle paymentCycle, int amountToPay, String paymentReference) {
        Payment actualPayment = verifyFailedPaymentSavedCorrectly(paymentCycle);
        assertThat(actualPayment.getPaymentAmountInPence()).isEqualTo(amountToPay);
        assertThat(actualPayment.getPaymentReference()).isEqualTo(paymentReference);
    }

    private Payment verifyFailedPaymentSavedCorrectly(PaymentCycle paymentCycle) {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1);
        Payment actualPayment = captor.getValue();
        assertThat(actualPayment.getCardAccountId()).isEqualTo(CARD_ACCOUNT_ID);
        assertThat(actualPayment.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(actualPayment.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(actualPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertThat(actualPayment.getPaymentTimestamp()).isNotNull();
        assertThat(actualPayment.getId()).isNotNull();
        return actualPayment;
    }

    private void verifyEventFailExceptionAndEventAreCorrect(PaymentCycle paymentCycle,
                                                            RuntimeException testException,
                                                            EventFailedException exception,
                                                            String expectedFailureMessage,
                                                            Integer totalEntitlementAmountInPence) {
        assertThat(exception).hasMessage(expectedFailureMessage);
        assertThat(exception).hasCause(testException);
        FailureEvent failureEvent = exception.getFailureEvent();
        assertThat(failureEvent.getEventType()).isEqualTo(CommonEventType.FAILURE);
        assertThat(failureEvent.getTimestamp()).isNotNull();
        Map<String, Object> metadata = failureEvent.getEventMetadata();
        assertThat(metadata.get(CLAIM_ID.getKey())).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(metadata.get(ENTITLEMENT_AMOUNT_IN_PENCE.getKey())).isEqualTo(paymentCycle.getTotalEntitlementAmountInPence());
        assertThat(metadata.get(PAYMENT_AMOUNT.getKey())).isEqualTo(totalEntitlementAmountInPence);
        assertThat(metadata.get(PAYMENT_ID.getKey())).isNotNull();
        assertThat(metadata.get(PAYMENT_REFERENCE.getKey())).isNull();
        assertThat(metadata.get(FAILED_EVENT_KEY)).isEqualTo(ClaimEventType.MAKE_PAYMENT);
        assertThat(metadata.get(FAILURE_DESCRIPTION_KEY)).isEqualTo(expectedFailureMessage);
        String actualExceptionDetail = (String) metadata.get(EXCEPTION_DETAIL_KEY);
        assertThat(actualExceptionDetail).startsWith("test exception");
        assertThat(actualExceptionDetail).contains("PaymentService.depositFundsToCard");
    }

    private void verifyDepositFundsRequestCorrectWithSpecificReference(int expectedEntitlementAmount, String expectedPaymentReference) {
        DepositFundsRequest depositFundsRequest = verifyDepositFundsRequestCorrect(expectedEntitlementAmount);
        assertThat(depositFundsRequest.getReference()).isEqualTo(expectedPaymentReference);
    }

    private void verifyDepositFundsRequestCorrectWithAnyReference(int expectedEntitlementAmount) {
        DepositFundsRequest depositFundsRequest = verifyDepositFundsRequestCorrect(expectedEntitlementAmount);
        assertThat(depositFundsRequest.getReference()).isNotNull();
    }

    private DepositFundsRequest verifyDepositFundsRequestCorrect(int expectedEntitlementAmount) {
        ArgumentCaptor<DepositFundsRequest> argumentCaptor = ArgumentCaptor.forClass(DepositFundsRequest.class);
        verify(cardClient).depositFundsToCard(eq(CARD_ACCOUNT_ID), argumentCaptor.capture());
        DepositFundsRequest depositFundsRequest = argumentCaptor.getValue();
        assertThat(depositFundsRequest.getAmountInPence()).isEqualTo(expectedEntitlementAmount);
        return depositFundsRequest;
    }

    private void verifyPaymentCycleStatusAndBalanceUpdated(PaymentCycle paymentCycle, int expectedBalance, PaymentCycleStatus paymentCycleStatus) {
        PaymentCycle savedPaymentCycle = verifyPaymentCycleStatusSaved(paymentCycle, paymentCycleStatus);
        assertThat(savedPaymentCycle.getCardBalanceInPence()).isEqualTo(expectedBalance);
        assertThat(savedPaymentCycle.getCardBalanceTimestamp()).isNotNull();
    }

    private PaymentCycle verifyPaymentCycleStatusSaved(PaymentCycle paymentCycle, PaymentCycleStatus paymentCycleStatus) {
        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleService).savePaymentCycle(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).hasSize(1);
        PaymentCycle savedPaymentCycle = argumentCaptor.getValue();
        assertThat(savedPaymentCycle.getId()).isEqualTo(paymentCycle.getId());
        assertThat(savedPaymentCycle.getPaymentCycleStatus()).isEqualTo(paymentCycleStatus);
        return savedPaymentCycle;
    }

    private void assertMessagePayload(MakePaymentMessagePayload messagePayload, PaymentCycle paymentCycle) {
        assertThat(messagePayload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(messagePayload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(messagePayload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
    }

    private void assertSuccessfulPayment(Payment result, PaymentCycle paymentCycle, int paymentAmount) {
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(result.getCardAccountId()).isEqualTo(CARD_ACCOUNT_ID);
        assertThat(result.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(result.getPaymentReference()).isEqualTo(CARD_PROVIDER_PAYMENT_REFERENCE);
        assertThat(result.getPaymentAmountInPence()).isEqualTo(paymentAmount);
        assertThat(result.getPaymentTimestamp()).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
