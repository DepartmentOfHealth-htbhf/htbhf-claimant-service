package uk.gov.dhsc.htbhf.claimant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.service.notify.NotificationClient;

import java.util.Map;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "notify")
public class EmailMessageConfiguration {

    @Getter
    @Setter
    private Map<EmailType, String> templateIds;

    @Bean
    public EmailTemplateConfig emailTemplateConfig() {
        return new EmailTemplateConfig(templateIds);
    }

    @Bean
    public NotificationClient notificationClient(@Value("${notify.api-key}") String notifyApiKey) {
        return new NotificationClient(notifyApiKey);
    }

}