package uk.gov.dhsc.htbhf.claimant.message.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.CompleteNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;
import uk.gov.dhsc.htbhf.claimant.testsupport.CardRequestTestDataFactory;
import uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory;
import uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EligibilityAndEntitlementTestDataFactory.anEligibleDecision;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.CARD_ACCOUNT_ID;

@ExtendWith(MockitoExtension.class)
class RequestNewCardMessageProcessorTest {

    @Mock
    MessageContextLoader messageContextLoader;
    @Mock
    CardRequestFactory cardRequestFactory;
    @Mock
    CardClient cardClient;
    @Mock
    MessageQueueClient messageQueueClient;

    @InjectMocks
    RequestNewCardMessageProcessor requestNewCardMessageProcessor;

    @Test
    void shouldRequestNewCardAndSendCompleteNewCardMessage() {
        Message message = MessageTestDataFactory.aValidMessageWithType(MessageType.REQUEST_NEW_CARD);
        Claim claim = aValidClaim();
        EligibilityAndEntitlementDecision decision = anEligibleDecision();
        RequestNewCardMessageContext context = RequestNewCardMessageContext.builder()
                .claim(claim)
                .eligibilityAndEntitlementDecision(decision)
                .build();
        given(messageContextLoader.loadRequestNewCardContext(any())).willReturn(context);
        CardRequest cardRequest = CardRequestTestDataFactory.aCardRequest(claim);
        given(cardRequestFactory.createCardRequest(any())).willReturn(cardRequest);
        given(cardClient.requestNewCard(any())).willReturn(CardResponseTestDataFactory.aCardResponse(CARD_ACCOUNT_ID));

        MessageStatus status = requestNewCardMessageProcessor.processMessage(message);

        assertThat(status).isEqualTo(MessageStatus.COMPLETED);
        verify(messageContextLoader).loadRequestNewCardContext(message);
        verify(cardRequestFactory).createCardRequest(claim);
        verify(cardClient).requestNewCard(cardRequest);
        MessagePayload expectedPayload = CompleteNewCardMessagePayload.builder()
                .cardAccountId(CARD_ACCOUNT_ID)
                .claimId(claim.getId())
                .eligibilityAndEntitlementDecision(decision)
                .build();
        verify(messageQueueClient).sendMessage(expectedPayload, MessageType.COMPLETE_NEW_CARD_PROCESS);
    }

}
