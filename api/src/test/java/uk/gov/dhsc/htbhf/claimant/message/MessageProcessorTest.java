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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.repository.MessageRepository;
import uk.gov.dhsc.htbhf.claimant.service.audit.EventAuditor;
import uk.gov.dhsc.htbhf.claimant.service.audit.NewCardEvent;
import uk.gov.dhsc.htbhf.logging.TestAppender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.ERROR;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.FAILED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.*;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithTypeAndTimestamp;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final int MESSAGE_PROCESSING_LIMIT = 10;
    private static final Pageable PAGEABLE = PageRequest.of(0, MESSAGE_PROCESSING_LIMIT);

    private MessageProcessor messageProcessor;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageStatusProcessor messageStatusProcessor;

    @Mock
    private EventAuditor eventAuditor;

    @Spy
    private CreateNewCardDummyMessageTypeProcessor createNewCardDummyMessageTypeProcessor = new CreateNewCardDummyMessageTypeProcessor();

    @Spy
    private SendFirstEmailDummyMessageTypeProcessor sendFirstEmailDummyMessageTypeProcessor = new SendFirstEmailDummyMessageTypeProcessor();

    @BeforeEach
    void init() {
        Map<MessageType, MessageTypeProcessor> messageTypeProcessorMap = Map.of(
                CREATE_NEW_CARD, createNewCardDummyMessageTypeProcessor,
                SEND_FIRST_EMAIL, sendFirstEmailDummyMessageTypeProcessor);
        messageProcessor = new MessageProcessor(messageStatusProcessor, messageRepository, eventAuditor, messageTypeProcessorMap, MESSAGE_PROCESSING_LIMIT);
    }

    @AfterEach
    void tearDown() {
        TestAppender.clearAllEvents();
    }

    //These calls need to be lenient as Mockito sees mocking the same method call multiple times as a sign of an error
    //and will fail the test without setting the mode to lenient.
    @Test
    void shouldCallDummyProcessors() {
        Message cardMessage = aValidMessage();
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE)).thenReturn(singletonList(cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(DETERMINE_ENTITLEMENT, PAGEABLE)).thenReturn(emptyList());
        //When
        messageProcessor.processAllMessages();
        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage);
        verify(sendFirstEmailDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(DETERMINE_ENTITLEMENT, PAGEABLE);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage, COMPLETED);
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor);
        verifyZeroInteractions(eventAuditor);
    }

    @Test
    void shouldContinueToProcessMessagesAfterAnExceptionThrown() {
        Message cardMessage1 = aValidMessageWithTimestamp(LocalDateTime.now().minusHours(1));
        Message cardMessage2 = aValidMessageWithTimestamp(LocalDateTime.now().minusHours(2));
        Message cardMessage3 = aValidMessageWithTimestamp(LocalDateTime.now().minusHours(3));
        given(messageRepository.findAllMessagesByTypeOrderedByDate(any(), any()))
                .willReturn(List.of(cardMessage1, cardMessage2, cardMessage3))
                .willReturn(emptyList());
        given(createNewCardDummyMessageTypeProcessor.processMessage(any()))
                .willThrow(new RuntimeException("foo"))
                .willReturn(FAILED)
                .willReturn(COMPLETED);

        //When
        messageProcessor.processAllMessages();

        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage1);
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage2);
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage3);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage1, ERROR);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage2, FAILED);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage3, COMPLETED);
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor, createNewCardDummyMessageTypeProcessor);
        verifyZeroInteractions(eventAuditor);
    }

    @Test
    void shouldAuditFailedEventWHenAnEventFailedExceptionIsThrown() {
        //Given
        Message cardMessage = aValidMessageWithTimestamp(LocalDateTime.now().minusHours(1));
        given(messageRepository.findAllMessagesByTypeOrderedByDate(any(), any()))
                .willReturn(List.of(cardMessage))
                .willReturn(emptyList());
        UUID claimId = UUID.randomUUID();
        NewCardEvent event = NewCardEvent.builder().claimId(claimId).build();
        String failureMessage = "Something went badly wrong";
        EventFailedException eventFailedException = new EventFailedException(event, new RuntimeException("test exception"), failureMessage);
        given(createNewCardDummyMessageTypeProcessor.processMessage(any())).willThrow(eventFailedException);

        //When
        messageProcessor.processAllMessages();

        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage, ERROR);
        verify(eventAuditor).auditFailedEvent(eventFailedException.getFailureEvent());
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor, createNewCardDummyMessageTypeProcessor, eventAuditor);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForMessageWithNoProcessor() {
        LocalDateTime originalTimestamp = LocalDateTime.now().minusHours(1);
        Message cardMessage = aValidMessageWithTypeAndTimestamp(MAKE_PAYMENT, originalTimestamp);
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE)).thenReturn(singletonList(cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL, PAGEABLE)).thenReturn(emptyList());
        //When
        IllegalArgumentException thrown = catchThrowableOfType(() -> messageProcessor.processAllMessages(), IllegalArgumentException.class);
        //Then
        assertThat(thrown).hasMessage("No message type processor found in application context for message type: "
                + "MAKE_PAYMENT, there are 1 message(s) in the queue");
        verify(sendFirstEmailDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(createNewCardDummyMessageTypeProcessor, never()).processMessage(any(Message.class));
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT, PAGEABLE);
        verify(messageRepository).findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE);
        verify(messageStatusProcessor).updateMessagesToErrorAndIncrementCount(singletonList(cardMessage));
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor);
        verifyZeroInteractions(eventAuditor);
    }

    @Test
    void shouldLogMessageProcessResults() {
        Message cardMessage = aValidMessageWithType(CREATE_NEW_CARD);
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE)).thenReturn(asList(cardMessage, cardMessage));
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL, PAGEABLE)).thenReturn(emptyList());
        //When
        messageProcessor.processAllMessages();
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("Processing 2 message(s) of type CREATE_NEW_CARD");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(events.get(1).getFormattedMessage()).isEqualTo("Processed 2 message(s) with status COMPLETED");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void shouldLogNullMessageProcessResult() {
        Message emailMessage = aValidMessageWithType(SEND_FIRST_EMAIL);
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(CREATE_NEW_CARD, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(MAKE_FIRST_PAYMENT, PAGEABLE)).thenReturn(emptyList());
        lenient().when(messageRepository.findAllMessagesByTypeOrderedByDate(SEND_FIRST_EMAIL, PAGEABLE)).thenReturn(singletonList(emailMessage));
        //When
        messageProcessor.processAllMessages();
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("Processing 1 message(s) of type SEND_FIRST_EMAIL");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
        // use startsWith instead of isEqualTo because the full class name contains mockito text.
        assertThat(events.get(1).getFormattedMessage()).startsWith("Received null message status from MessageTypeProcessor:"
                + " uk.gov.dhsc.htbhf.claimant.message.SendFirstEmailDummyMessageTypeProcessor");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.ERROR);
    }

}
