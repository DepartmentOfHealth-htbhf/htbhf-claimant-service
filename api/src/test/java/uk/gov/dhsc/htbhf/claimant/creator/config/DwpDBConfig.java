package uk.gov.dhsc.htbhf.claimant.creator.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Configuration class for connecting to the dwp database.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "dwpEntityManagerFactory",
        basePackages = {"uk.gov.dhsc.htbhf.claimant.creator.dwp.repository"},
        transactionManagerRef = "dwpTransactionManager")
@Profile("test-claimant-creator")
public class DwpDBConfig {

    @Bean(name = "dwpDataSource")
    @ConfigurationProperties(prefix = "dwp")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dwpEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                       @Qualifier("dwpDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("uk.gov.dhsc.htbhf.claimant.creator.dwp.entities")
                .persistenceUnit("dwp")
                .build();
    }

    @Bean(name = "dwpTransactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dwpEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
