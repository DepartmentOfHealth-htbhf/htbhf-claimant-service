package uk.gov.dhsc.htbhf.claimant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.dhsc.htbhf.requestcontext.HeaderInterceptor;
import uk.gov.dhsc.htbhf.requestcontext.RequestContext;

/**
 * Configuration for scheduling the creation of new cards.
 */
@Configuration
public class SchedulerConfig {

    public static final String SCHEDULER_REST_TEMPLATE_QUALIFIER = "schedulerRestTemplate";
    public static final String SCHEDULER_REQUEST_CONTEXT = "schedulerRequestContext";

    @Bean(name = SCHEDULER_REQUEST_CONTEXT)
    public RequestContext schedulerRequestContext() {
        return new RequestContext();
    }

    /**
     * Creates a rest template which is outside the scope of web requests. Required as
     * quartz jobs are not web request scoped.
     * @return the rest template
     */
    @Bean(name = SCHEDULER_REST_TEMPLATE_QUALIFIER)
    public RestTemplate schedulerRestTemplate(@Qualifier(SCHEDULER_REQUEST_CONTEXT) RequestContext requestContext) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new HeaderInterceptor(requestContext));
        return restTemplate;
    }

}
