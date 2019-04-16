package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;

import java.util.List;
import java.util.UUID;

/**
 * Job that calls the {@link NewCardService} to create new cards on a scheduled basis.
 * Concurrent execution is disabled so only one instance of this job will run at any time. This is needed
 * to prevent multiple jobs trying to update the same data and potentially create more than one card per claim.
 */
@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution
public class CreateCardJob extends QuartzJobBean {

    private NewCardService newCardService;
    private ClaimRepository claimRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Starting create card job with id {}", context.getFireInstanceId());

        List<UUID> newClaimIds = claimRepository.getNewClaimIds();
        newClaimIds.forEach(claimId -> newCardService.createNewCards(claimId));
    }
}
