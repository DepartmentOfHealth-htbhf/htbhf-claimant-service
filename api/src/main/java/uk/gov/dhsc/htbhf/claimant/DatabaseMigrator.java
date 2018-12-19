package uk.gov.dhsc.htbhf.claimant;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Component which performs the database migration process on startup, current
 * implementation is usinf FLyway which checks for new migration scripts in the
 * db.migration folder in the htbhf-claimant-service-db project.
 */
@Component
public class DatabaseMigrator {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrator.class.getName());

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void performMigration() {
        LOGGER.info("************* Performing Flyway migration on db");
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        LOGGER.info("************* Finished Flyway migration");
    }

}
