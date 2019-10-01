package uk.gov.dhsc.htbhf.claimant.creator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.gov.dhsc.htbhf.claimant.converter.AddressDTOToAddressConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@Profile("test-claimant-creator")
public class ClaimantCreator {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Bean
    public AddressDTOToAddressConverter addressDTOToAddressConverter() {
        return new AddressDTOToAddressConverter();
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ClaimantCreator.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("test-claimant-creator");
        application.run(args);
    }
}
