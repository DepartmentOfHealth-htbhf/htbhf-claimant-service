package uk.gov.dhsc.htbhf.claimant.service.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.model.ClaimStatus;
import uk.gov.dhsc.htbhf.claimant.model.card.CardResponse;
import uk.gov.dhsc.htbhf.eligibility.model.EligibilityStatus;
import uk.gov.dhsc.htbhf.logging.EventLogger;
import uk.gov.dhsc.htbhf.logging.event.Event;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.service.audit.ClaimEventType.NEW_CLAIM;
import static uk.gov.dhsc.htbhf.claimant.testsupport.CardResponseTestDataFactory.aCardResponse;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;

@ExtendWith(MockitoExtension.class)
class ClaimAuditorTest {

    @Mock
    private EventLogger eventLogger;

    @InjectMocks
    private ClaimAuditor claimAuditor;

    @Test
    void shouldLogEventForValidClaimant() {
        //Given
        Claim claim = aValidClaim();
        //When
        claimAuditor.auditNewClaim(claim);
        //Then
        UUID claimId = claim.getId();
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        assertThat(eventArgumentCaptor.getAllValues()).hasSize(1);
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(NEW_CLAIM);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(3)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId),
                        entry(ClaimEventMetadataKey.CLAIM_STATUS.getKey(), ClaimStatus.ACTIVE),
                        entry(ClaimEventMetadataKey.ELIGIBILITY_STATUS.getKey(), EligibilityStatus.ELIGIBLE));
    }

    @Test
    void shouldNotLogEventForNullClaimant() {
        //When
        claimAuditor.auditNewClaim(null);
        //Then
        verifyZeroInteractions(eventLogger);
    }

    @Test
    void shouldLogEventForNewCard() {
        //Given
        UUID claimId = UUID.randomUUID();
        CardResponse cardResponse = aCardResponse();

        //When
        claimAuditor.auditNewCard(claimId, cardResponse);

        //Then
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventLogger).logEvent(eventArgumentCaptor.capture());
        assertThat(eventArgumentCaptor.getAllValues()).hasSize(1);
        Event actualEvent = eventArgumentCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(NEW_CARD);
        assertThat(actualEvent.getTimestamp()).isNotNull();
        assertThat(actualEvent.getEventMetadata())
                .isNotNull()
                .hasSize(2)
                .containsExactly(
                        entry(ClaimEventMetadataKey.CARD_ACCOUNT_ID.getKey(), cardResponse.getCardAccountId()),
                        entry(ClaimEventMetadataKey.CLAIM_ID.getKey(), claimId));

    }
}
