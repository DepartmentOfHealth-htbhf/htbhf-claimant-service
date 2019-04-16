package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import uk.gov.dhsc.htbhf.claimant.repository.ClaimRepository;
import uk.gov.dhsc.htbhf.claimant.service.NewCardService;
import uk.gov.dhsc.htbhf.requestcontext.RequestContext;

import java.util.List;
import java.util.UUID;

import static uk.gov.dhsc.htbhf.claimant.SchedulerConfig.SCHEDULER_REQUEST_CONTEXT;

/**
 * Job that calls the {@link NewCardService} to create new cards on a scheduled basis.
 * Concurrent execution is disabled so only one instance of this job will run at any time. This is needed
 * to prevent multiple jobs trying to update the same data and potentially create more than one card per claim.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class CreateCardJob extends QuartzJobBean {

    private final NewCardService newCardService;
    private final ClaimRepository claimRepository;
    private final RequestContext requestContext;

    public CreateCardJob(@Qualifier(SCHEDULER_REQUEST_CONTEXT) RequestContext requestContext,
                         NewCardService newCardService,
                         ClaimRepository claimRepository) {
        this.newCardService = newCardService;
        this.claimRepository = claimRepository;
        this.requestContext = requestContext;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Starting create card job with id {}", context.getFireInstanceId());
        requestContext.setSessionId(context.getFireInstanceId());

        List<UUID> newClaimIds = claimRepository.getNewClaimIds();
        newClaimIds.forEach(newCardService::createNewCards);
    }
}
