package uk.gov.dhsc.htbhf.claimant.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class CreateCardJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Starting create card job with id {}", context.getFireInstanceId());

        try {
            ApplicationContext applicationContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
            NewCardScheduleService newCardScheduleService = applicationContext.getBean(NewCardScheduleService.class);
            newCardScheduleService.createNewCards();
        } catch (SchedulerException e) {
            log.error("An error occurred whilst creating new cards", e);
        }
    }
}
