package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution
public class CreateCardJob extends QuartzJobBean {

    private NewCardScheduleService newCardScheduleService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Starting create card job with id {}", context.getFireInstanceId());

        newCardScheduleService.createNewCards();
    }
}
