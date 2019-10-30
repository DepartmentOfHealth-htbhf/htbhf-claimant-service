package uk.gov.dhsc.htbhf.claimant.creator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.gov.dhsc.htbhf.claimant.creator.config.DWPMediator;
import uk.gov.dhsc.htbhf.claimant.testsupport.RepositoryMediator;

@SpringBootApplication
@Profile("test-claimant-creator")
public class DatabaseFastForwarder {

    @Bean
    public RepositoryMediator repositoryMediator() {
        return new RepositoryMediator();
    }


    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DatabaseFastForwarder.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("test-claimant-creator");
        ConfigurableApplicationContext context = application.run(args);
        RepositoryMediator repositoryMediator = context.getBean(RepositoryMediator.class);
        repositoryMediator.ageDatabaseEntities(28);
        DWPMediator dwpMediator = context.getBean(DWPMediator.class);
        dwpMediator.ageDatabaseEntities(28);
    }
}
