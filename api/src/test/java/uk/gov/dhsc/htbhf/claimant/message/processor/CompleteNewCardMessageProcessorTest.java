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
import uk.gov.dhsc.htbhf.claimant.message.context.CompleteNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.MakePaymentMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.claim.ClaimActivationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.model.ClaimStatus.ACTIVE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PaymentCycleTestDataFactory.aPaymentCycleWithClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

@ExtendWith(MockitoExtension.class)
class CompleteNewCardMessageProcessorTest {

    @Mock
    MessageContextLoader messageContextLoader;
    @Mock
    ClaimActivationService claimActivationService;
    @Mock
    MessageQueueClient messageQueueClient;

    @InjectMocks
    private CompleteNewCardMessageProcessor completeNewCardMessageProcessor;

    @Test
    void shouldProcessCompleteNewCardMessage() {
        //Given
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = anEligibleDecision();
        CompleteNewCardMessageContext context = CompleteNewCardMessageContext.builder()
                .claim(claim)
                .eligibilityAndEntitlementDecision(decision)
                .cardAccountId(CARD_ACCOUNT_ID)
                .build();
        given(messageContextLoader.loadCompleteNewCardContext(any())).willReturn(context);
        PaymentCycle paymentCycle = aPaymentCycleWithClaim(claim);
        given(claimActivationService.updateClaimAndCreatePaymentCycle(any(), any(), any())).willReturn(paymentCycle);
        Message message = aValidMessage();

        //When
        MessageStatus status = completeNewCardMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        assertThat(claim.getCardAccountId()).isEqualTo(context.getCardAccountId());
        assertThat(claim.getClaimStatus()).isEqualTo(ACTIVE);

        verify(messageContextLoader).loadCompleteNewCardContext(message);
        verify(claimActivationService).updateClaimAndCreatePaymentCycle(claim, CARD_ACCOUNT_ID, decision);
        verifyMakeFirstPaymentMessageSent(claim, paymentCycle);
    }

    private void verifyMakeFirstPaymentMessageSent(Claim claim, PaymentCycle paymentCycle) {
        ArgumentCaptor<MessagePayload> payloadCaptor = ArgumentCaptor.forClass(MessagePayload.class);
        verify(messageQueueClient).sendMessage(payloadCaptor.capture(), eq(MessageType.MAKE_FIRST_PAYMENT));
        assertThat(payloadCaptor.getValue()).isInstanceOf(MakePaymentMessagePayload.class);
        MakePaymentMessagePayload payload = (MakePaymentMessagePayload) payloadCaptor.getValue();
        assertThat(payload.getCardAccountId()).isEqualTo(claim.getCardAccountId());
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getPaymentCycleId()).isEqualTo(paymentCycle.getId());
    }

}
