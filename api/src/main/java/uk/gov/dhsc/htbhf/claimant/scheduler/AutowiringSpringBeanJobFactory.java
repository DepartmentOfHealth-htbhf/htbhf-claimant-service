package uk.gov.dhsc.htbhf.claimant.scheduler;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * {@link org.quartz.spi.JobFactory} implementation that is spring aware, allowing {@link org.quartz.Job} instances
 * to autowire spring bean dependencies.
 * See <a href="https://dzone.com/articles/spring-and-quartz-integration-that-works-together">here</a> for a more complex example
 * which gives the job access to {@link org.quartz.JobDataMap}s and the {@link org.quartz.SchedulerContext}. (not need for our job)
 */
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Retrieves a job from the {@link ApplicationContext}.
     * @param bundle the trigger fired bundle containing the job to create.
     * @return the created job.
     */
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) {
        return applicationContext.getBean(bundle.getJobDetail().getJobClass());
    }
}
