package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.communications.EmailMessagePayloadFactory.createEmailPersonalisationWithFirstAndLastNameOnly;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;

@Component
@AllArgsConstructor
public class DetermineEntitlementNotificationHandler {

    private final MessageQueueClient messageQueueClient;

    public void sendClaimNoLongerEligibleEmail(Claim claim) {
        MessagePayload messagePayload = buildClaimIsNoLongerEligibleNotificationEmailPayload(claim);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }

    public void sendNoChildrenOnFeedClaimNoLongerEligibleEmail(Claim claim) {
        MessagePayload messagePayload = buildNoChildrenOnFeedClaimIsNoLongerEligibleNotificationEmailPayload(claim);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }

    private EmailMessagePayload buildClaimIsNoLongerEligibleNotificationEmailPayload(Claim claim) {
        return buildNoLongerEligibleEmailPayload(claim, EmailType.CLAIM_NO_LONGER_ELIGIBLE);
    }

    private EmailMessagePayload buildNoChildrenOnFeedClaimIsNoLongerEligibleNotificationEmailPayload(Claim claim) {
        return buildNoLongerEligibleEmailPayload(claim, EmailType.NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE);
    }

    private EmailMessagePayload buildNoLongerEligibleEmailPayload(Claim claim, EmailType emailType) {
        Map<String, Object> emailPersonalisation = createEmailPersonalisationWithFirstAndLastNameOnly(claim.getClaimant());
        return EmailMessagePayload.builder()
                .claimId(claim.getId())
                .emailType(emailType)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }
}
