package uk.gov.dhsc.htbhf.claimant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;

@Configuration
public class EmailMessageConfiguration {

    @Bean
    public NotificationClient notificationClient(@Value("${notify.api-key}") String notifyApiKey) {
        return new NotificationClient(notifyApiKey);
    }

}
