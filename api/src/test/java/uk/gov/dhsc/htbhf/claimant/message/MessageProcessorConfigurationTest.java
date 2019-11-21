package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;

import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;

@SpringJUnitConfig(classes = {MessageProcessorConfigurationTest.TestConfig.class, MessageProcessorConfiguration.class})
@TestPropertySource(properties = {
        "message-processor.message-limit=100",
})
@ExtendWith(MockitoExtension.class)
class MessageProcessorConfigurationTest {

    @Autowired
    private MessageProcessor messageProcessor;
    @MockBean
    private MessageRepository messageRepository;
    @MockBean
    private MessageStatusProcessor messageStatusProcessor;
    @MockBean
    private EventAuditor eventAuditor;

    @Test
    void shouldBuildMessageTypeProcessorMapPostConstruction() {
        //Given the application context is built using the TestConfig below

        //Then
        @SuppressWarnings("unchecked")
        Map<MessageType, MessageTypeProcessor> allMessageProcessorsByType = (Map<MessageType, MessageTypeProcessor>)
                ReflectionTestUtils.getField(messageProcessor, "messageProcessorsByType");
        assertThat(allMessageProcessorsByType).hasSize(2);
        assertThat(allMessageProcessorsByType.containsKey(REQUEST_NEW_CARD)).isTrue();
        assertThat(allMessageProcessorsByType.containsKey(MAKE_PAYMENT)).isFalse();
        assertThat(allMessageProcessorsByType.containsKey(SEND_EMAIL)).isTrue();
        assertThat(allMessageProcessorsByType.get(REQUEST_NEW_CARD)).isInstanceOf(CreateNewCardDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(SEND_EMAIL)).isInstanceOf(SendEmailDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(MAKE_PAYMENT)).isNull();
        verifyNoMoreInteractions(messageRepository);
    }

    //This test specifically doesn't use the Configuration in the context as we cannot trap the Exception being caught
    //if there are no MessageTypeProcessors available in the context, so call the method directly with a new MessageProcessorConfiguration
    @Test
    void shouldThrownBeanCreationExceptionIfNoMessageTypeProcessorsProvided() {
        //Given
        MessageProcessorConfiguration configuration = new MessageProcessorConfiguration();
        //When
        BeanCreationException thrown = catchThrowableOfType(
                () -> configuration.messageProcessor(emptyList(), messageRepository, messageStatusProcessor, eventAuditor, 100),
                BeanCreationException.class);
        //Then
        assertThat(thrown).hasMessage("Unable to create MessageProcessor, no MessageTypeProcessor instances found");
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
        SendEmailDummyMessageTypeProcessor sendFirstEmailDummyMessageTypeProcessor() {
            return new SendEmailDummyMessageTypeProcessor();
        }
    }

}
