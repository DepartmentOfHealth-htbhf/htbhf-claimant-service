package uk.gov.dhsc.htbhf.claimant.message;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory;
import uk.gov.dhsc.htbhf.claimant.testsupport.TestAppender;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.CREATE_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_FIRST_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_FIRST_EMAIL;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private MessageProcessor messageProcessor;

    @Mock
    private MessageRepository messageRepository;

    @Spy
    private CreateNewCardDummyMessageTypeProcessor createNewCardDummyMessageTypeProcessor = new CreateNewCardDummyMessageTypeProcessor();

    @Spy
    private SendFirstEmailDummyMessageTypeProcessor sendFirstEmailDummyMessageTypeProcessor = new SendFirstEmailDummyMessageTypeProcessor();

    @BeforeEach
    void init() {
        Map<MessageType, MessageTypeProcessor> messageTypeProcessorMap = Map.of(
                CREATE_NEW_CARD, createNewCardDummyMessageTypeProcessor,
                SEND_FIRST_EMAIL, sendFirstEmailDummyMessageTypeProcessor);
        messageProcessor = new MessageProcessor(messageRepository, messageTypeProcessorMap);
    }

    @AfterEach
    void tearDown() {
        TestAppender.clearAllEvents();
    }

    //These calls need to be lenient as Mockito sees mocking the same method call multiple times as a sign of an error
    //and will fail the test without setting the mode to lenient.
    @Test
    void shouldCallDummyProcessors() {
        Message cardMessage = MessageTestDataFactory.aValidMessage();
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).thenReturn(singletonList(cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).thenReturn(emptyList());
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
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).thenReturn(singletonList(cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).thenReturn(emptyList());
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

    @Test
    void shouldLogMessageProcessResults() {
        Message cardMessage = MessageTestDataFactory.aValidMessageWithType(CREATE_NEW_CARD);
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).thenReturn(asList(cardMessage, cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).thenReturn(emptyList());
        //When
        messageProcessor.processAllMessages();
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events.size()).isEqualTo(1);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("Processed 2 messages with status COMPLETED");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void shouldLogNullMessageProcessResult() {
        Message emailMessage = MessageTestDataFactory.aValidMessageWithType(SEND_FIRST_EMAIL);
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL)).thenReturn(singletonList(emailMessage));
        //When
        messageProcessor.processAllMessages();
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events.size()).isEqualTo(1);
        // use startsWith instead of isEqualTo because the full class name contains mockito text.
        assertThat(events.get(0).getFormattedMessage()).startsWith("Received null message status from MessageTypeProcessor:"
                + " uk.gov.dhsc.htbhf.claimant.message.SendFirstEmailDummyMessageTypeProcessor");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}
