package uk.gov.dhsc.htbhf.claimant.service.payments;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentCycleRepository;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.MakePaymentEvent;
import uk.gov.dhsc.htbhf.logging.event.FailureEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.entity.PaymentStatus.SUCCESS;
import static uk.gov.dhsc.htbhf.claimant.reporting.PaymentAction.INITIAL_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifyFailedPaymentEventFailExceptionAndEventCorrect;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.FailureEventTestDataFactory.aFailureEventWithEvent;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCalculationTestDataFactory.aFullPaymentCalculation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentResultTestDataFactory.aValidPaymentResult;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureEmbeddedDatabase
@Transactional
class PaymentServiceTest {

    private static final String CARD_PROVIDER_PAYMENT_REFERENCE = "cardProviderPaymentReference";

    @MockBean
    private CardClient cardClient;

    @MockBean
    private EventAuditor eventAuditor;

    @MockBean
    private PaymentCycleService paymentCycleService;

    @MockBean
    private PaymentCalculator paymentCalculator;

    @MockBean
    private ReportPaymentMessageSender reportPaymentMessageSender;

    @Autowired
    private PaymentCycleRepository paymentCycleRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        paymentCycleRepository.deleteAll();
        claimRepository.deleteAll();
    }

    @Test
    void shouldGetBalance() {
        PaymentCycle paymentCycle = createAndSavePaymentCycle();
        CardBalanceResponse cardBalanceResponse = aValidCardBalanceResponse();
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        given(cardClient.getBalance(any())).willReturn(cardBalanceResponse);
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentCalculation);

        PaymentCalculation result = paymentService.calculatePaymentAmount(paymentCycle);

        assertThat(result).isEqualTo(paymentCalculation);
        verify(cardClient).getBalance(paymentCycle.getClaim().getCardAccountId());
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), cardBalanceResponse.getAvailableBalanceInPence());
    }

    @Test
    void shouldMakePayment() {
        PaymentCycle paymentCycle = createAndSavePaymentCycle();
        int paymentAmountInPence = 100;
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        PaymentResult paymentResult = paymentService.makePayment(paymentCycle, paymentAmountInPence, INITIAL_PAYMENT);

        assertSuccessfulPayment(paymentResult);
        verify(eventAuditor).auditMakePayment(eq(paymentCycle), eq(paymentAmountInPence), anyString(), eq(CARD_PROVIDER_PAYMENT_REFERENCE));
        verifyDepositFundsRequestCorrectWithSpecificReference(paymentAmountInPence, paymentResult.getRequestReference());
        verify(reportPaymentMessageSender).sendReportPaymentMessage(paymentCycle.getClaim(), paymentCycle, INITIAL_PAYMENT);
    }

    @Test
    void shouldAuditFailedPaymentWhenPaymentFails() {
        PaymentCycle paymentCycle = createAndSavePaymentCycle();
        given(cardClient.depositFundsToCard(any(), any())).willThrow(TEST_EXCEPTION);
        int paymentAmountInPence = 100;

        EventFailedException exception = catchThrowableOfType(
                () -> paymentService.makePayment(paymentCycle, paymentAmountInPence, INITIAL_PAYMENT),
                EventFailedException.class);

        verifyFailedPaymentEventFailExceptionAndEventCorrect(
                paymentCycle,
                TEST_EXCEPTION,
                exception,
                paymentAmountInPence
        );
        verifyNoInteractions(paymentCycleService, eventAuditor, reportPaymentMessageSender);
        verifyDepositFundsRequestCorrectWithAnyReference(paymentAmountInPence);
    }

    @Test
    void shouldCompletePayment() {
        PaymentCycle paymentCycle = createAndSavePaymentCycle();
        PaymentCalculation paymentCalculation = aFullPaymentCalculation();
        PaymentResult paymentResult = aValidPaymentResult();

        paymentService.completePayment(paymentCycle, paymentCalculation, paymentResult);

        verify(paymentCycleService).updatePaymentCycleFromCalculation(paymentCycle, paymentCalculation);
        assertThat(paymentRepository.count()).isEqualTo(1);
        Payment payment = paymentRepository.findAll().iterator().next();
        assertThat(payment.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(payment.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(payment.getPaymentStatus()).isEqualTo(SUCCESS);
        assertThat(payment.getPaymentTimestamp()).isNotNull();
        assertThat(payment.getPaymentAmountInPence()).isEqualTo(paymentCalculation.getPaymentAmount());
        assertThat(payment.getRequestReference()).isEqualTo(paymentResult.getRequestReference());
        assertThat(payment.getResponseReference()).isEqualTo(paymentResult.getResponseReference());
    }

    @Test
    void shouldSaveFailedPayment() {
        PaymentCycle paymentCycle = createAndSavePaymentCycle();
        int amountToPay = 1240;
        String requestReference = "paymentRef1";
        MakePaymentEvent event = MakePaymentEvent.builder()
                .claimId(paymentCycle.getClaim().getId())
                .paymentAmountInPence(amountToPay)
                .requestReference(requestReference)
                .build();
        FailureEvent failureEvent = aFailureEventWithEvent(event);

        paymentService.saveFailedPayment(paymentCycle, failureEvent);

        verifyFailedPaymentSavedWithAllData(paymentCycle, amountToPay, requestReference, failureEvent);
        verifyNoInteractions(reportPaymentMessageSender);
    }

    private PaymentCycle createAndSavePaymentCycle() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        claimRepository.save(paymentCycle.getClaim());
        paymentCycleRepository.save(paymentCycle);
        return paymentCycle;
    }

    private void verifyFailedPaymentSavedWithAllData(PaymentCycle paymentCycle, int amountToPay, String paymentRequestReference, FailureEvent failureEvent) {
        Payment actualPayment = verifyFailedPaymentSavedCorrectly(paymentCycle, failureEvent);
        assertThat(actualPayment.getPaymentAmountInPence()).isEqualTo(amountToPay);
        assertThat(actualPayment.getRequestReference()).isEqualTo(paymentRequestReference);
        assertThat(actualPayment.getFailureDetail()).isEqualTo(failureEvent.getEventMetadata().get(FailureEvent.EXCEPTION_DETAIL_KEY));
    }

    private Payment verifyFailedPaymentSavedCorrectly(PaymentCycle paymentCycle, FailureEvent failureEvent) {
        Payment actualPayment = paymentRepository.findAll().iterator().next();
        assertThat(actualPayment.getCardAccountId()).isEqualTo(CARD_ACCOUNT_ID);
        assertThat(actualPayment.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(actualPayment.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(actualPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILURE);
        assertThat(actualPayment.getPaymentTimestamp()).isEqualTo(failureEvent.getTimestamp());
        assertThat(actualPayment.getId()).isNotNull();
        return actualPayment;
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

    private void assertSuccessfulPayment(PaymentResult result) {
        assertThat(result).isNotNull();
        assertThat(result.getRequestReference()).isNotNull();
        assertThat(result.getResponseReference()).isEqualTo(CARD_PROVIDER_PAYMENT_REFERENCE);
        assertThat(result.getPaymentTimestamp()).isNotNull();
    }
}
