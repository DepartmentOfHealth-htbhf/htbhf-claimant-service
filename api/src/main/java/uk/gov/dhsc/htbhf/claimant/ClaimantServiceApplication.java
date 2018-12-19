package uk.gov.dhsc.htbhf.claimant;

import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class ClaimantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimantServiceApplication.class, args);
    }

    @Bean
    @SuppressWarnings("PMD.LawOfDemeter")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).load();
    }
}
