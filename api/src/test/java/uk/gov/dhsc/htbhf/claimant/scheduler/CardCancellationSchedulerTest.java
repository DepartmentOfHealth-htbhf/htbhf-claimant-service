package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.entity.CardStatus.PENDING_CANCELLATION;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aClaimWithCardStatusAndCardStatusTimestamp;

@ExtendWith(MockitoExtension.class)
class CardCancellationSchedulerTest {

    private static final Period GRACE_PERIOD = Period.ofWeeks(16);

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private HandleCardPendingCancellationJob job;

    private CardCancellationScheduler cardCancellationScheduler;

    @BeforeEach
    void init() {
        cardCancellationScheduler = new CardCancellationScheduler(GRACE_PERIOD, claimRepository, job);
    }

    @Test
    public void shouldSendEmailAndSetCardStatusWhenClaimHasBeenPendingCancellationFor16Weeks() {
        LocalDateTime sixteenWeeksAgo = LocalDateTime.now().minus(GRACE_PERIOD);
        Claim claim = aClaimWithCardStatusAndCardStatusTimestamp(PENDING_CANCELLATION, sixteenWeeksAgo);
        given(claimRepository.getClaimsWithCardStatusPendingCancellationOlderThan(any())).willReturn(List.of(claim));

        cardCancellationScheduler.handleCardsToBeCancelled();

        verify(claimRepository).getClaimsWithCardStatusPendingCancellationOlderThan(GRACE_PERIOD);
        verify(job).handleCardPendingCancellation(claim);
    }

    @Test
    void shouldContinueProcessingAfterException() {
        LocalDateTime sixteenWeeksAgo = LocalDateTime.now().minus(GRACE_PERIOD);
        Claim claim1 = aClaimWithCardStatusAndCardStatusTimestamp(PENDING_CANCELLATION, sixteenWeeksAgo);
        Claim claim2 = aClaimWithCardStatusAndCardStatusTimestamp(PENDING_CANCELLATION, sixteenWeeksAgo);
        given(claimRepository.getClaimsWithCardStatusPendingCancellationOlderThan(any())).willReturn(List.of(claim1, claim2));
        // throw an exception then run without throwing an exception
        doThrow(new RuntimeException("test"))
                .doNothing()
                .when(job).handleCardPendingCancellation(any());

        cardCancellationScheduler.handleCardsToBeCancelled();

        verify(claimRepository).getClaimsWithCardStatusPendingCancellationOlderThan(GRACE_PERIOD);
        verify(job).handleCardPendingCancellation(claim1);
        verify(job).handleCardPendingCancellation(claim2);
        verifyNoMoreInteractions(job);
    }
}
