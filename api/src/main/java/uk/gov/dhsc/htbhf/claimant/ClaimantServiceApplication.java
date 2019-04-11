package uk.gov.dhsc.htbhf.claimant;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.dhsc.htbhf.CommonRestConfiguration;
import uk.gov.dhsc.htbhf.claimant.scheduler.CreateCardJob;

/**
 * The starting point for spring boot, this class enables SpringFox for documenting the api using swagger
 * and defines a number of beans.
 * See also: {@link ApiDocumentation}.
 */
@SpringBootApplication
@EnableSwagger2
@Import(CommonRestConfiguration.class)
@SuppressWarnings("PMD.UseUtilityClass")
public class ClaimantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimantServiceApplication.class, args);
    }

    @Bean
    public JobDetail createCardJobDetail() {
        return JobBuilder.newJob(CreateCardJob.class)
                .withIdentity("Create card")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger createCardTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(5).repeatForever();

        return TriggerBuilder.newTrigger().forJob(createCardJobDetail())
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setTriggers(createCardTrigger());
        schedulerFactoryBean.setJobDetails(createCardJobDetail());
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        return schedulerFactoryBean;
    }
}
