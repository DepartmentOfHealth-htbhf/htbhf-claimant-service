package uk.gov.dhsc.htbhf.claimant;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import uk.gov.dhsc.htbhf.claimant.scheduler.AutowiringSpringBeanJobFactory;
import uk.gov.dhsc.htbhf.claimant.scheduler.CreateCardJob;

import javax.sql.DataSource;

@Configuration
public class SchedulerConfig {

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
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
                .withIntervalInHours(1).repeatForever();

        return TriggerBuilder.newTrigger().forJob(createCardJobDetail())
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobDetails(createCardJobDetail());
        factory.setTriggers(createCardTrigger());
        factory.setDataSource(dataSource);
        factory.setJobFactory(jobFactory);
        factory.setConfigLocation(new ClassPathResource("quartz.properties"));
        return factory;
    }
}
