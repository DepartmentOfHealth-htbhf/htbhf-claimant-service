package uk.gov.dhsc.htbhf.claimant.message.processor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.factory.CardRequestFactory;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.MessageTypeProcessor;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.context.RequestNewCardMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.payload.CompleteNewCardMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.model.card.CardRequest;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.claimant.model.eligibility.EligibilityAndEntitlementDecision;
import uk.gov.dhsc.htbhf.claimant.service.CardClient;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.COMPLETE_NEW_CARD_PROCESS;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;

/**
 * Responsible for processing {@link MessageType#REQUEST_NEW_CARD} messages by:
 * Creating a new card,
 * Creating a PaymentCycle for the claim,
 * Sending a MAKE_FIRST_PAYMENT message.
 */
@Component
@AllArgsConstructor
@Slf4j
public class RequestNewCardMessageProcessor implements MessageTypeProcessor {

    private MessageContextLoader messageContextLoader;
    private CardClient cardClient;
    private CardRequestFactory cardRequestFactory;
    private MessageQueueClient messageQueueClient;

    @Override
    public MessageType supportsMessageType() {
        return REQUEST_NEW_CARD;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public MessageStatus processMessage(Message message) {
        RequestNewCardMessageContext context = messageContextLoader.loadRequestNewCardContext(message);
        Claim claim = context.getClaim();
        CardRequest cardRequest = cardRequestFactory.createCardRequest(claim);
        CardResponse cardResponse = cardClient.requestNewCard(cardRequest);
        sendCompleteNewCardMessage(claim, cardResponse.getCardAccountId(), context.getEligibilityAndEntitlementDecision());
        return COMPLETED;
    }

    private void sendCompleteNewCardMessage(Claim claim, String cardAccountId, EligibilityAndEntitlementDecision decision) {
        MessagePayload payload = CompleteNewCardMessagePayload.builder()
                .cardAccountId(cardAccountId)
                .claimId(claim.getId())
                .eligibilityAndEntitlementDecision(decision)
                .build();
        messageQueueClient.sendMessage(payload, COMPLETE_NEW_CARD_PROCESS);
    }

}
