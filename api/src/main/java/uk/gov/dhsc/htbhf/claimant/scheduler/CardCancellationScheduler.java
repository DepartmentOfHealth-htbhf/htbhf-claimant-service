package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.requestcontext.aop.NewRequestContextWithSessionId;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import static uk.gov.dhsc.htbhf.logging.ExceptionDetailGenerator.constructExceptionDetail;

/**
 * Responsible for notifying claimants that their card is soon to be cancelled and creating a cancel card message.
 * All claims that have been in PENDING_CANCELLATION for 16 weeks or more will be updated to SCHEDULED_FOR_CANCELLATION
 * and have a notification sent.
 */
@Slf4j
@Component
public class CardCancellationScheduler {

    private final Period gracePeriod;
    private final ClaimRepository claimRepository;
    private final HandleCardPendingCancellationJob job;

    public CardCancellationScheduler(@Value("${card-cancellation.grace-period}") Period gracePeriod,
                                     ClaimRepository claimRepository,
                                     HandleCardPendingCancellationJob job) {
        this.gracePeriod = gracePeriod;
        this.claimRepository = claimRepository;
        this.job = job;
    }

    @Scheduled(cron = "${card-cancellation.schedule.cron-schedule}")
    @SchedulerLock(
            name = "Set cards to be cancelled",
            lockAtLeastForString = "${card-cancellation.schedule.minimum-lock-time}",
            lockAtMostForString = "${card-cancellation.schedule.maximum-lock-time}")
    @NewRequestContextWithSessionId(sessionId = "CardCancellationScheduler")
    public void handleCardsPendingCancellation() {
        log.debug("Querying for claims with a card status of pending cancellation since {}", LocalDate.now().minus(gracePeriod));
        List<Claim> claims = claimRepository.getClaimsWithCardStatusPendingCancellationOlderThan(gracePeriod);

        if (claims.isEmpty()) {
            log.debug("No cards to be scheduled for cancellation");
        } else {
            log.debug("Processing {} cards to be scheduled for cancellation", claims.size());
            claims.forEach(this::handleCardPendingCancellation);
            log.debug("Finished scheduling {} cards for cancellation", claims.size());
        }
    }

    private void handleCardPendingCancellation(Claim claim) {
        try {
            job.handleCardPendingCancellation(claim);
        } catch (RuntimeException e) {
            log.error("Unable to handle card pending cancellation for claim {}: {}", claim.getId(), constructExceptionDetail(e), e);
        }
    }
}
