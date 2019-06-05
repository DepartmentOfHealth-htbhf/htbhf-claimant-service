package uk.gov.dhsc.htbhf.claimant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Configuration used when deployed in a cloud environment, e.g. cloud foundry.
 */
@Configuration
@Profile("cloud")
public class CloudDBConfiguration extends AbstractCloudConfig {

    @Value("${spring.datasource.hikari.connectionTimeout}")
    private Integer connectionTimeout;

    @Value("${spring.datasource.hikari.minimum-idle}")
    private Integer minimumIdle;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private Integer maximumPoolSize;

    @Value("${postgres.service.id}")
    private String serviceId;

    /**
     * Create our own datasource with {@link org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig}, otherwise
     * cloud foundry will using default settings which sets the maximum pool size to 4.
     * See https://cloud.spring.io/spring-cloud-connectors/spring-cloud-spring-service-connector.html and
     * https://github.com/cloudfoundry/java-buildpack-auto-reconfiguration
     */
    @Bean
    public DataSource dataSource() {
        PooledServiceConnectorConfig.PoolConfig poolConfig = new PooledServiceConnectorConfig.PoolConfig(minimumIdle, maximumPoolSize, connectionTimeout);
        DataSourceConfig dbConfig = new DataSourceConfig(poolConfig, null);
        return connectionFactory().dataSource(serviceId, dbConfig);
    }
}
