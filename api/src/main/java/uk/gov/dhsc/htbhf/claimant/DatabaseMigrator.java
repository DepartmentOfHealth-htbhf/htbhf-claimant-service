package uk.gov.dhsc.htbhf.claimant;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Component which performs the database migration process on startup, current
 * implementation is using Flyway which checks for new migration scripts in the
 * db.migration folder in the htbhf-claimant-service-db project.
 */
@Component
@Slf4j
public class DatabaseMigrator {

    @Autowired
    private Flyway flyway;

    @PostConstruct
    public void performMigration() {
        log.info("************* Performing Flyway migration on db");
        flyway.migrate();
        log.info("************* Finished Flyway migration");
    }

}
