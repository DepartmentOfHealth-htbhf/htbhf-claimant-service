package uk.gov.dhsc.htbhf.claimant

import org.flywaydb.core.Flyway
import spock.lang.Specification

class DatabaseMigratorSpec extends Specification {

    DatabaseMigrator databaseMigrator = new DatabaseMigrator()
    Flyway flyway = Mock()

    def "Should invoke flyway to migrate database"() {
        when: "The method is invoked on @PostConstruct"
        databaseMigrator.performMigration()

        then:
        1 * flyway.migrate()
    }
}
