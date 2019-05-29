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
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueDAO;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.CardBalanceResponse;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.DepositFundsResponse;
import uk.gov.dhsc.htbhf.claimant.repository.PaymentRepository;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardBalanceResponseTestDataFactory.aValidCardBalanceResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aValidPaymentCycle;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.AVAILABLE_BALANCE_IN_PENCE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

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
        ArgumentCaptor<DepositFundsRequest> argumentCaptor = ArgumentCaptor.forClass(DepositFundsRequest.class);
        verify(cardClient).depositFundsToCard(eq(CARD_ACCOUNT_ID), argumentCaptor.capture());
        DepositFundsRequest depositFundsRequest = argumentCaptor.getValue();
        assertThat(depositFundsRequest.getAmountInPence()).isEqualTo(paymentCycle.getTotalEntitlementAmountInPence());
        assertThat(depositFundsRequest.getReference()).isEqualTo(paymentResult.getId().toString());
    }

    @Test
    void shouldMakePayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        int paymentAmount = 1240;
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentAmount);
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        Payment paymentResult = paymentService.makePayment(paymentCycle, CARD_ACCOUNT_ID);

        assertSuccessfulPayment(paymentResult, paymentCycle, paymentAmount);
        verify(paymentRepository).save(paymentResult);
        verify(eventAuditor).auditMakePayment(paymentCycle, paymentResult, depositFundsResponse);
        verify(cardClient).getBalance(CARD_ACCOUNT_ID);
        verifyPaymentCycleSavedWithBalance(paymentCycle, balanceResponse.getAvailableBalanceInPence());
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        ArgumentCaptor<DepositFundsRequest> argumentCaptor = ArgumentCaptor.forClass(DepositFundsRequest.class);
        verify(cardClient).depositFundsToCard(eq(CARD_ACCOUNT_ID), argumentCaptor.capture());
        DepositFundsRequest depositFundsRequest = argumentCaptor.getValue();
        assertThat(depositFundsRequest.getAmountInPence()).isEqualTo(paymentAmount);
        assertThat(depositFundsRequest.getReference()).isEqualTo(paymentResult.getId().toString());
    }

    @Test
    void shouldMakeNoPaymentWhenBalanceIsTooHigh() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        int paymentAmount = 0;
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentAmount);

        Payment paymentResult = paymentService.makePayment(paymentCycle, CARD_ACCOUNT_ID);

        assertThat(paymentResult).isNull();
        verify(cardClient).getBalance(CARD_ACCOUNT_ID);
        verifyPaymentCycleSavedWithBalance(paymentCycle, balanceResponse.getAvailableBalanceInPence());
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        verifyNoMoreInteractions(cardClient);
        verifyZeroInteractions(paymentRepository);
        verify(eventAuditor).auditBalanceTooHighForPayment(
                paymentCycle.getClaim().getId(),
                paymentCycle.getTotalEntitlementAmountInPence(),
                paymentCycle.getCardBalanceInPence());
        verifyNoMoreInteractions(eventAuditor);
    }

    private void verifyPaymentCycleSavedWithBalance(PaymentCycle paymentCycle, int expectedBalance) {
        ArgumentCaptor<PaymentCycle> argumentCaptor = ArgumentCaptor.forClass(PaymentCycle.class);
        verify(paymentCycleService).savePaymentCycle(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).hasSize(1);
        PaymentCycle savedPaymentCycle = argumentCaptor.getValue();
        assertThat(savedPaymentCycle.getId()).isEqualTo(paymentCycle.getId());
        assertThat(savedPaymentCycle.getCardBalanceInPence()).isEqualTo(expectedBalance);
        assertThat(savedPaymentCycle.getCardBalanceTimestamp()).isNotNull();
        assertThat(savedPaymentCycle.getPaymentCycleStatus()).isEqualTo(PaymentCycleStatus.FULL_PAYMENT_MADE);
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
