package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.message.MessageQueueClient;
import uk.gov.dhsc.htbhf.claimant.message.MessageType;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailMessagePayload;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.SCHEDULED_FOR_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.message.EmailPayloadAssertions.assertEmailPayloadCorrectWithFirstAndLastName;
import static uk.gov.dhsc.htbhf.claimant.message.payload.EmailType.CARD_IS_ABOUT_TO_BE_CANCELLED;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithCardStatusAndCardStatusTimestamp;

@ExtendWith(MockitoExtension.class)
class HandleCardPendingCancellationJobTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private MessageQueueClient messageQueueClient;

    @InjectMocks
    private HandleCardPendingCancellationJob job;

    @Test
    public void shouldSendCardToBeCancelledEmailAndSetCardStatus() {
        Claim claim = aClaimWithCardStatusAndCardStatusTimestamp(PENDING_CANCELLATION, LocalDateTime.now().minusWeeks(16));

        job.handleCardPendingCancellation(claim);

        assertEmailSent(claim);
        assertClaimCardStatusUpdated();
    }

    private void assertEmailSent(Claim claim) {
        ArgumentCaptor<EmailMessagePayload> argumentCaptor = ArgumentCaptor.forClass(EmailMessagePayload.class);
        verify(messageQueueClient).sendMessage(argumentCaptor.capture(), eq(MessageType.SEND_EMAIL));
        assertEmailPayloadCorrectWithFirstAndLastName(argumentCaptor.getValue(), claim, CARD_IS_ABOUT_TO_BE_CANCELLED);
    }

    private void assertClaimCardStatusUpdated() {
        ArgumentCaptor<Claim> argumentCaptor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getCardStatus()).isEqualTo(SCHEDULED_FOR_CANCELLATION);
    }
}
