package uk.gov.dhsc.htbhf.claimant.communications;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Claimant;
import uk.gov.dhsc.htbhf.claimant.message.EmailTemplateKey;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.dhsc.htbhf.claimant.message.payload.MessagePayload;

import java.util.Map;

import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;

@Component
@AllArgsConstructor
public class ClaimEmailHandler {

    private final MessageQueueClient messageQueueClient;

    public void sendClaimNoLongerEligibleEmail(Claim claim) {
        MessagePayload messagePayload = buildClaimIsNoLongerEligibleNotificationEmailPayload(claim);
        messageQueueClient.sendMessage(messagePayload, SEND_EMAIL);
    }

    private EmailMessagePayload buildClaimIsNoLongerEligibleNotificationEmailPayload(Claim claim) {
        Map<String, Object> emailPersonalisation = createClaimNoLongerEligibleEmailPersonalisationMap(claim.getClaimant());
        return EmailMessagePayload.builder()
                .claimId(claim.getId())
                .emailType(EmailType.CLAIM_NO_LONGER_ELIGIBLE)
                .emailPersonalisation(emailPersonalisation)
                .build();
    }

    private static Map<String, Object> createClaimNoLongerEligibleEmailPersonalisationMap(Claimant claimant) {
        return Map.of(
                EmailTemplateKey.FIRST_NAME.getTemplateKeyName(), claimant.getFirstName(),
                EmailTemplateKey.LAST_NAME.getTemplateKeyName(), claimant.getLastName()
        );
    }
}
