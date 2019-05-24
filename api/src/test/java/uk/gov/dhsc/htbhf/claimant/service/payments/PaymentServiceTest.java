package uk.gov.dhsc.htbhf.claimant.service.payments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Payment;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
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
        String cardAccountId = "myCardAccountId";
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        Payment result = paymentService.makeFirstPayment(paymentCycle, cardAccountId);

        assertSuccessfulPayment(result, paymentCycle, cardAccountId, paymentCycle.getTotalEntitlementAmountInPence());
        verify(paymentRepository).save(result);
        verify(eventAuditor).auditMakePayment(paymentCycle.getClaim().getId(), result.getId(), CARD_PROVIDER_PAYMENT_REFERENCE);
        ArgumentCaptor<DepositFundsRequest> argumentCaptor = ArgumentCaptor.forClass(DepositFundsRequest.class);
        verify(cardClient).depositFundsToCard(eq(cardAccountId), argumentCaptor.capture());
        DepositFundsRequest depositFundsRequest = argumentCaptor.getValue();
        assertThat(depositFundsRequest.getAmountInPence()).isEqualTo(paymentCycle.getTotalEntitlementAmountInPence());
        assertThat(depositFundsRequest.getReference()).isEqualTo(result.getId().toString());
    }

    @Test
    void shouldMakePayment() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        String cardAccountId = "myCardAccountId";
        DepositFundsResponse depositFundsResponse = DepositFundsResponse.builder().referenceId(CARD_PROVIDER_PAYMENT_REFERENCE).build();
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        int paymentAmount = 1240;
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentAmount);
        given(cardClient.depositFundsToCard(any(), any())).willReturn(depositFundsResponse);

        Payment result = paymentService.makePayment(paymentCycle, cardAccountId);

        assertSuccessfulPayment(result, paymentCycle, cardAccountId, paymentAmount);
        verify(paymentRepository).save(result);
        verify(eventAuditor).auditMakePayment(paymentCycle.getClaim().getId(), result.getId(), CARD_PROVIDER_PAYMENT_REFERENCE);
        verify(cardClient).getBalance(cardAccountId);
        verify(paymentCycleService).updateAndSavePaymentCycleWithBalance(paymentCycle, balanceResponse);
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        ArgumentCaptor<DepositFundsRequest> argumentCaptor = ArgumentCaptor.forClass(DepositFundsRequest.class);
        verify(cardClient).depositFundsToCard(eq(cardAccountId), argumentCaptor.capture());
        DepositFundsRequest depositFundsRequest = argumentCaptor.getValue();
        assertThat(depositFundsRequest.getAmountInPence()).isEqualTo(paymentAmount);
        assertThat(depositFundsRequest.getReference()).isEqualTo(result.getId().toString());
    }

    @Test
    void shouldMakeNoPaymentWhenBalanceIsTooHigh() {
        PaymentCycle paymentCycle = aValidPaymentCycle();
        String cardAccountId = "myCardAccountId";
        CardBalanceResponse balanceResponse = aValidCardBalanceResponse();
        given(cardClient.getBalance(any())).willReturn(balanceResponse);
        int paymentAmount = 0;
        given(paymentCalculator.calculatePaymentCycleAmountInPence(any(), anyInt())).willReturn(paymentAmount);

        Payment result = paymentService.makePayment(paymentCycle, cardAccountId);

        assertThat(result).isNull();
        verify(cardClient).getBalance(cardAccountId);
        verify(paymentCycleService).updateAndSavePaymentCycleWithBalance(paymentCycle, balanceResponse);
        verify(paymentCalculator).calculatePaymentCycleAmountInPence(paymentCycle.getVoucherEntitlement(), AVAILABLE_BALANCE_IN_PENCE);
        verifyNoMoreInteractions(cardClient);
        verifyZeroInteractions(paymentRepository, eventAuditor);
    }

    private void assertMessagePayload(MakePaymentMessagePayload messagePayload, PaymentCycle paymentCycle) {
        assertThat(messagePayload.getClaimId()).isEqualTo(paymentCycle.getClaim().getId());
        assertThat(messagePayload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
        assertThat(messagePayload.getCardAccountId()).isEqualTo(paymentCycle.getClaim().getCardAccountId());
    }

    private void assertSuccessfulPayment(Payment result, PaymentCycle paymentCycle, String cardAccountId, int paymentAmount) {
        assertThat(result).isNotNull();
        assertThat(result.getClaim()).isEqualTo(paymentCycle.getClaim());
        assertThat(result.getCardAccountId()).isEqualTo(cardAccountId);
        assertThat(result.getPaymentCycle()).isEqualTo(paymentCycle);
        assertThat(result.getPaymentReference()).isEqualTo(CARD_PROVIDER_PAYMENT_REFERENCE);
        assertThat(result.getPaymentAmountInPence()).isEqualTo(paymentAmount);
        assertThat(result.getPaymentTimestamp()).isNotNull();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
