package uk.gov.dhsc.htbhf.claimant.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory;

import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_FIRST_EMAIL;

@SpringJUnitConfig(MessageProcessorTest.TestConfig.class)
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    @Autowired
    private MessageProcessor messageProcessor;

    @MockBean
    private MessageRepository messageRepository;

    @SpyBean
    private CreateNewCardDummyMessageTypeProcessor createNewCardDummyMessageTypeProcessor;

    @SpyBean
    private SendFirstEmailDummyMessageTypeProcessor sendFirstEmailDummyMessageTypeProcessor;

    @Test
    void shouldBuildMessageTypeProcessorMapPostConstruction() {
        //Given the application context is built using the TestConfig below

        //Then
        Map<MessageType, MessageTypeProcessor> allMessageProcessorsByType = (Map<MessageType, MessageTypeProcessor>)
                ReflectionTestUtils.getField(messageProcessor, "allMessageProcessorsByType");
        assertThat(allMessageProcessorsByType).hasSize(2);
        assertThat(allMessageProcessorsByType.containsKey(CREATE_NEW_CARD)).isTrue();
        assertThat(allMessageProcessorsByType.containsKey(MAKE_FIRST_PAYMENT)).isFalse();
        assertThat(allMessageProcessorsByType.containsKey(SEND_FIRST_EMAIL)).isTrue();
        assertThat(allMessageProcessorsByType.get(CREATE_NEW_CARD)).isInstanceOf(CreateNewCardDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(SEND_FIRST_EMAIL)).isInstanceOf(SendFirstEmailDummyMessageTypeProcessor.class);
        assertThat(allMessageProcessorsByType.get(MAKE_FIRST_PAYMENT)).isNull();
        verifyZeroInteractions(messageRepository, createNewCardDummyMessageTypeProcessor, sendFirstEmailDummyMessageTypeProcessor);
    }

    @Test
    void shouldCallDummyProcessors() {
        Message cardMessage = MessageTestDataFactory.aValidMessage();
        given(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).willReturn(singletonList(cardMessage));
        given(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).willReturn(emptyList());
        given(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).willReturn(emptyList());
        //When
        messageProcessor.processAllMessages();
        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage);
        verify(sendFirstEmailDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL);
        verifyNoMoreInteractions(messageRepository);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForMessageWithNoProcessor() {
        Message cardMessage = MessageTestDataFactory.aValidMessageWithType(MAKE_FIRST_PAYMENT);
        given(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).willReturn(emptyList());
        given(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).willReturn(singletonList(cardMessage));
        given(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).willReturn(emptyList());
        //When
        IllegalArgumentException thrown = catchThrowableOfType(() -> messageProcessor.processAllMessages(), IllegalArgumentException.class);
        //Then
        assertThat(thrown).hasMessage("No message type processor found in application context for message type: "
                + "MAKE_FIRST_PAYMENT, there are 1 message(s) in the queue");
        verify(sendFirstEmailDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(createNewCardDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT);
        verifyNoMoreInteractions(messageRepository);
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

    public static class CreateNewCardDummyMessageTypeProcessor implements MessageTypeProcessor {

        @Override
        public MessageStatus processMessage(Message message) {
            return null;
        }

        @Override
        public MessageType supportsMessageType() {
            return CREATE_NEW_CARD;
        }
    }

    public static class SendFirstEmailDummyMessageTypeProcessor implements MessageTypeProcessor {

        @Override
        public MessageStatus processMessage(Message message) {
            return null;
        }

        @Override
        public MessageType supportsMessageType() {
            return SEND_FIRST_EMAIL;
        }
    }

}
