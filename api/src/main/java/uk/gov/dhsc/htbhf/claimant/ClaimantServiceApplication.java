package uk.gov.dhsc.htbhf.claimant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.dhsc.htbhf.CommonRestConfiguration;
import uk.gov.dhsc.htbhf.claimant.message.MessageProcessorConfiguration;
import uk.gov.dhsc.htbhf.logging.EventLogger;
import uk.gov.dhsc.htbhf.logging.LoggingConfiguration;
import uk.gov.dhsc.htbhf.logging.event.ApplicationStartedEvent;

import java.time.Clock;

/**
 * The starting point for spring boot, this class enables SpringFox for documenting the api using swagger
 * and defines a number of beans.
 * See also: {@link ApiDocumentation}.
 */
@SpringBootApplication
@EnableSwagger2
@Configuration
@Import({CommonRestConfiguration.class, LoggingConfiguration.class, MessageProcessorConfiguration.class})
public class ClaimantServiceApplication {

    @Value("${app.version:}") // use APP_VERSION env variable if available, otherwise give no version info
    private String appVersion;

    @Value("${instance.index:}") // use INSTANCE_INDEX env variable if available, otherwise give no index info
    private String instanceIndex;

    @Value("${vcap.application.application_id:}") // the id of the application as provided by cf
    private String applicationId;

    @Autowired
    private EventLogger eventLogger;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logAfterStartup() {
        eventLogger.logEvent(ApplicationStartedEvent.builder()
                .applicationId(applicationId)
                .applicationVersion(appVersion)
                .instanceIndex(instanceIndex)
                .build()
        );
    }

    public static void main(String[] args) {
        SpringApplication.run(ClaimantServiceApplication.class, args);
    }

}
