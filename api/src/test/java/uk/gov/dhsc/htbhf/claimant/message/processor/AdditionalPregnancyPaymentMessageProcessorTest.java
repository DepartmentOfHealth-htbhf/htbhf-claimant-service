package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.AdditionalPregnancyVoucherCalculator;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.reporting.ReportPaymentMessageSender;
import uk.gov.dhsc.htbhf.claimant.service.payments.PaymentService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.ADDITIONAL_PREGNANCY_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageContextTestDataFactory.aValidAdditionalPregnancyPaymentMessageContext;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithCycleEntitlementAndClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleVoucherEntitlementTestDataFactory.aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VOUCHER_VALUE_IN_PENCE;

@ExtendWith(MockitoExtension.class)
class AdditionalPregnancyPaymentMessageProcessorTest {

    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentService paymentService;
    @Mock
    private AdditionalPregnancyVoucherCalculator voucherCalculator;
    @Mock
    private ReportPaymentMessageSender reportPaymentMessageSender;

    private AdditionalPregnancyPaymentMessageProcessor messageProcessor;

    @BeforeEach
    public void setUp() {
        messageProcessor = new AdditionalPregnancyPaymentMessageProcessor(
                VOUCHER_VALUE_IN_PENCE,
                messageContextLoader,
                voucherCalculator,
                paymentService,
                reportPaymentMessageSender);
    }

    @Test
    void shouldProcessMessageAndMakePaymentWhenCurrentPaymentCycleHasNoPregnancyVouchers() {
        int numberOfVouchers = 2;
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);

        shouldCalculateAdditionalVouchers(numberOfVouchers, claim, paymentCycle);

        int paymentAmount = numberOfVouchers * VOUCHER_VALUE_IN_PENCE;
        verify(paymentService).makeInterimPayment(paymentCycle, claim.getCardAccountId(), paymentAmount);
        verify(reportPaymentMessageSender).sendReportPregnancyTopUpPaymentMessage(claim, paymentCycle, paymentAmount);
    }

    @Test
    void shouldProcessMessageAndMakeNoPaymentWhenThereAreNoAdditionalVouchers() {
        int numberOfVouchers = 0;
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithoutPregnancyVouchers();
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);

        shouldCalculateAdditionalVouchers(numberOfVouchers, claim, paymentCycle);

        verifyZeroInteractions(paymentService, reportPaymentMessageSender);
    }

    private void shouldCalculateAdditionalVouchers(int numberOfVouchers, Claim claim, PaymentCycle paymentCycle) {
        Message message = aValidMessageWithType(ADDITIONAL_PREGNANCY_PAYMENT);
        AdditionalPregnancyPaymentMessageContext context = aValidAdditionalPregnancyPaymentMessageContext(claim, Optional.of(paymentCycle));
        given(messageContextLoader.loadAdditionalPregnancyPaymentMessageContext(any())).willReturn(context);
        given(voucherCalculator.getAdditionalPregnancyVouchers(any(), any(), any())).willReturn(numberOfVouchers);

        MessageStatus messageStatus = messageProcessor.processMessage(message);

        assertThat(messageStatus).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadAdditionalPregnancyPaymentMessageContext(message);
        verify(voucherCalculator)
                .getAdditionalPregnancyVouchers(claim.getClaimant().getExpectedDeliveryDate(), paymentCycle, message.getProcessAfter().toLocalDate());
    }

    @Test
    void shouldProcessMessageAndNotMakePaymentWhenCurrentPaymentCycleHasPregnancyVouchers() {
        Message message = aValidMessageWithType(ADDITIONAL_PREGNANCY_PAYMENT);
        Claim claim = aValidClaim();
        PaymentCycleVoucherEntitlement voucherEntitlement = aPaymentCycleVoucherEntitlementWithPregnancyVouchers();
        PaymentCycle paymentCycle = aPaymentCycleWithCycleEntitlementAndClaim(voucherEntitlement, claim);
        AdditionalPregnancyPaymentMessageContext context = aValidAdditionalPregnancyPaymentMessageContext(claim, Optional.of(paymentCycle));
        given(messageContextLoader.loadAdditionalPregnancyPaymentMessageContext(any())).willReturn(context);

        MessageStatus messageStatus = messageProcessor.processMessage(message);

        assertThat(messageStatus).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadAdditionalPregnancyPaymentMessageContext(message);
        verifyZeroInteractions(voucherCalculator, paymentService, reportPaymentMessageSender);
    }

    @Test
    void shouldProcessMessageAndNotMakePaymentWhenClaimHasNoPaymentCycles() {
        Message message = aValidMessageWithType(ADDITIONAL_PREGNANCY_PAYMENT);
        Claim claim = aValidClaim();
        Optional<PaymentCycle> paymentCycle = Optional.empty();
        AdditionalPregnancyPaymentMessageContext context = aValidAdditionalPregnancyPaymentMessageContext(claim, paymentCycle);
        given(messageContextLoader.loadAdditionalPregnancyPaymentMessageContext(any())).willReturn(context);

        MessageStatus messageStatus = messageProcessor.processMessage(message);

        assertThat(messageStatus).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadAdditionalPregnancyPaymentMessageContext(message);
        verifyZeroInteractions(voucherCalculator, paymentService, reportPaymentMessageSender);
    }
}
