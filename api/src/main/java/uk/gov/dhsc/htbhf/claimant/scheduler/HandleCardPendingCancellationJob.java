package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import javax.transaction.Transactional;

import static uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory.buildEmailMessagePayloadWithFirstAndLastNameOnly;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.SCHEDULED_FOR_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.CARD_IS_ABOUT_TO_BE_CANCELLED;

@Component
@AllArgsConstructor
@Slf4j
public class HandleCardPendingCancellationJob {

    private MessageQueueClient messageQueueClient;
    private final ClaimRepository claimRepository;

    /**
     * Notifies claimants that their card is soon to be cancelled and sets the claim's card status to SCHEDULED_FOR_CANCELLATION.
     * @param claim the claim whose's card is to be scheduled for cancellation
     */
    @Transactional
    public void handleCardPendingCancellation(Claim claim) {
        MessagePayload messagePayload = buildEmailMessagePayloadWithFirstAndLastNameOnly(claim, CARD_IS_ABOUT_TO_BE_CANCELLED);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
        claim.updateCardStatus(SCHEDULED_FOR_CANCELLATION);
        claimRepository.save(claim);
    }
}
