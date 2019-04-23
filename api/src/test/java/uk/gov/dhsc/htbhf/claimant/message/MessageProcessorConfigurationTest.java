package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_FIRST_EMAIL;

@SpringJUnitConfig(classes = {MessageProcessorConfigurationTest.TestConfig.class, MessageProcessorConfiguration.class})
@ExtendWith(MockitoExtension.class)
class MessageProcessorConfigurationTest {

    @Autowired
    private MessageProcessor messageProcessor;
    @MockBean
    private MessageRepository messageRepository;

    @Test
    void shouldBuildMessageTypeProcessorMapPostConstruction() {
        //Given the application context is built using the TestConfig below

        //Then
        Map<MessageType, MessageTypeProcessor> allMessageProcessorsByType = (Map<MessageType, MessageTypeProcessor>)
                ReflectionTestUtils.getField(messageProcessor, "messageProcessorsByType");
        assertThat(allMessageProcessorsByType).hasSize(2);
        assertThat(allMessageProcessorsByType.containsKey(CREATE_NEW_CARD)).isTrue();
        assertThat(allMessageProcessorsByType.containsKey(MAKE_FIRST_PAYMENT)).isFalse();
        assertThat(allMessageProcessorsByType.containsKey(SEND_FIRST_EMAIL)).isTrue();
        assertThat(allMessageProcessorsByType.get(CREATE_NEW_CARD)).isInstanceOf(CreateNewCardDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(SEND_FIRST_EMAIL)).isInstanceOf(SendFirstEmailDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(MAKE_FIRST_PAYMENT)).isNull();
        verifyZeroInteractions(messageRepository);
    }

    @Configuration
    @ComponentScan(
            basePackageClasses = MessageProcessor.class,
            useDefaultFilters = false,
            includeFilters = {
                    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = MessageProcessor.class)
            })
    public static class TestConfig {

        @Bean
        CreateNewCardDummyMessageTypeProcessor createNewCardDummyMessageTypeProcessor() {
            return new CreateNewCardDummyMessageTypeProcessor();
        }

        @Bean
        SendFirstEmailDummyMessageTypeProcessor sendFirstEmailDummyMessageTypeProcessor() {
            return new SendFirstEmailDummyMessageTypeProcessor();
        }
    }

}
