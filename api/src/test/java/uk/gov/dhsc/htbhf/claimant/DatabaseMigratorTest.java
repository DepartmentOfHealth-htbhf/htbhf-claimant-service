package uk.gov.dhsc.htbhf.claimant;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatabaseMigratorTest {

    @Mock
    Flyway flyway;

    @InjectMocks
    DatabaseMigrator databaseMigrator;

    @Test
    void shouldInvokeFlywayDBMigration() {
        // Given

        // When
        databaseMigrator.performMigration();

        // Then
        verify(flyway).migrate();
    }

}