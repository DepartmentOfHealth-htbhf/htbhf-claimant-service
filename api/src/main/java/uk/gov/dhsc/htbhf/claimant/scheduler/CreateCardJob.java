package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.util.List;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class CreateCardJob {

    private ClaimRepository claimRepository;
    private NewCardService newCardService;

    @Scheduled(cron = "${card.schedule.new-card-interval-cron}")
    @SchedulerLock(
            name = "CreateNewCardsJob",
            lockAtLeastForString = "${card.schedule.minimum-lock-time}",
            lockAtMostForString = "${card.schedule.maximum-lock-time}")
    public void createNewCards() {
        log.info("Starting scheduled job to create new cards");

        List<UUID> claimIds = claimRepository.getNewClaimIds();
        claimIds.forEach(newCardService::createNewCard);
    }
}
