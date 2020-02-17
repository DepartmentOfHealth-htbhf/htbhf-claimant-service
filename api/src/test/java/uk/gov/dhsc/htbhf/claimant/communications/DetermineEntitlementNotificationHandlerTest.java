package uk.gov.dhsc.htbhf.claimant.communications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectWithFirstAndLastName;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class DetermineEntitlementNotificationHandlerTest {

    @Mock
    private MessageQueueClient messageQueueClient;

    @InjectMocks
    private DetermineEntitlementNotificationHandler determineEntitlementNotificationHandler;

    @Test
    public void shouldPutClaimNoLongerEligibleMessageOnQueue() {
        Claim claim = aValidClaim();

        determineEntitlementNotificationHandler.sendClaimNoLongerEligibleEmailIfPresent(claim);

        ArgumentCaptor<EmailMessagePayload> argumentCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(SEND_EMAIL));
        EmailMessagePayload payload = argumentCaptor.getValue();
        assertThat(payload.getEmailType()).isEqualTo(EmailType.CLAIM_NO_LONGER_ELIGIBLE);
        assertThat(payload.getClaimId()).isEqualTo(claim.getId());
        assertThat(payload.getEmailPersonalisation())
                .containsOnly(
                        entry("First_name", claim.getClaimant().getFirstName()),
                        entry("Last_name", claim.getClaimant().getLastName()));
    }

    @Test
    public void shouldNotPutClaimNoLongerEligibleMessageOnQueueForMissingEmail() {
        Claim claim = aValidClaim();
        claim.getClaimant().setEmailAddress(null);
        determineEntitlementNotificationHandler.sendClaimNoLongerEligibleEmailIfPresent(claim);
        verifyNoInteractions(messageQueueClient);
    }

    @Test
    public void shouldPutNoChildrenOnFeedClaimNoLongerEligibleMessageOnQueue() {
        Claim claim = aValidClaim();

        determineEntitlementNotificationHandler.sendNoChildrenOnFeedClaimNoLongerEligibleEmailIfPresent(claim);

        ArgumentCaptor<EmailMessagePayload> argumentCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(SEND_EMAIL));
        EmailMessagePayload payload = argumentCaptor.getValue();
        assertEmailPayloadCorrectWithFirstAndLastName(payload, claim, NO_CHILD_ON_FEED_NO_LONGER_ELIGIBLE);
    }

    @Test
    public void shouldNotPutNoChildrenOnFeedClaimNoLongerEligibleMessageOnQueue() {
        Claim claim = aValidClaim();
        claim.getClaimant().setEmailAddress(null);
        determineEntitlementNotificationHandler.sendNoChildrenOnFeedClaimNoLongerEligibleEmailIfPresent(claim);
        verifyNoInteractions(messageQueueClient);

    }
}
