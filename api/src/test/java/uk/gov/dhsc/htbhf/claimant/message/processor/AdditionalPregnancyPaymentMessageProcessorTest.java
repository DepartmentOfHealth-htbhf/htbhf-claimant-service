package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entitlement.PaymentCycleVoucherEntitlement;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.entity.PaymentCycle;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.AdditionalPregnancyPaymentMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
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

@ExtendWith(MockitoExtension.class)
class AdditionalPregnancyPaymentMessageProcessorTest {

    @Mock
    private MessageContextLoader messageContextLoader;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private AdditionalPregnancyPaymentMessageProcessor messageProcessor;

    @Test
    void shouldProcessMessageAndMakePaymentWhenCurrentPaymentCycleHasNoPregnancyVouchers() {}

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
        verifyZeroInteractions(paymentService);
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
        verifyZeroInteractions(paymentService);
    }
}
