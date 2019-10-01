package uk.gov.dhsc.htbhf.claimant.creator.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Configuration class for connecting to the claimant database.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "claimantEntityManagerFactory",
        basePackages = {"uk.gov.dhsc.htbhf.claimant.entity"},
        transactionManagerRef = "claimantTransactionManager")
@Profile("test-claimant-creator")
public class ClaimantDBConfig {

    @Primary
    @Bean(name = "claimantDataSource")
    @ConfigurationProperties(prefix = "claimant")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "claimantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("claimantDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("uk.gov.dhsc.htbhf.claimant.repository")
                .persistenceUnit("claimant")
                .build();
    }

    @Primary
    @Bean(name = "claimantTransactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("claimantEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
