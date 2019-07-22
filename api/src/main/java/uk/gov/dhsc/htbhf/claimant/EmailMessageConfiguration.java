package uk.gov.dhsc.htbhf.claimant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;

import java.util.Map;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "notify")
@Getter
@Setter
public class EmailMessageConfiguration {

    private Map<EmailType, String> templateIds;

    @Bean
    public EmailTemplateConfig emailTemplateConfig() {
        return new EmailTemplateConfig(templateIds);
    }

}
