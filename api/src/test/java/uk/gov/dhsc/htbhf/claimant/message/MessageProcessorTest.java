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
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.MAKE_PAYMENT;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.REQUEST_NEW_CARD;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aMessageWithMessageTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessage;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithTypeAndTimestamp;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.TEST_EXCEPTION;

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
    private SendEmailDummyMessageTypeProcessor sendEmailDummyMessageTypeProcessor = new SendEmailDummyMessageTypeProcessor();

    @BeforeEach
    void init() {
        Map<MessageType, MessageTypeProcessor> messageTypeProcessorMap = Map.of(
                REQUEST_NEW_CARD, createNewCardDummyMessageTypeProcessor,
                SEND_EMAIL, sendEmailDummyMessageTypeProcessor);
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
        lenient().when(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(REQUEST_NEW_CARD, PAGEABLE)).thenReturn(singletonList(cardMessage));
        //When
        messageProcessor.processMessagesOfType(REQUEST_NEW_CARD);
        //Then
        verify(messageRepository).findAllMessagesOfTypeWithTimestampBeforeNow(REQUEST_NEW_CARD, PAGEABLE);
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage, COMPLETED);
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor);
        verifyNoInteractions(eventAuditor);
    }

    @Test
    void shouldContinueToProcessMessagesAfterAnExceptionThrown() {
        Message cardMessage1 = aMessageWithMessageTimestamp(LocalDateTime.now().minusHours(1));
        Message cardMessage2 = aMessageWithMessageTimestamp(LocalDateTime.now().minusHours(2));
        Message cardMessage3 = aMessageWithMessageTimestamp(LocalDateTime.now().minusHours(3));
        given(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(any(), any()))
                .willReturn(List.of(cardMessage1, cardMessage2, cardMessage3))
                .willReturn(emptyList());
        given(createNewCardDummyMessageTypeProcessor.processMessage(any()))
                .willThrow(new RuntimeException("foo"))
                .willReturn(FAILED)
                .willReturn(COMPLETED);

        //When
        messageProcessor.processMessagesOfType(REQUEST_NEW_CARD);

        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage1);
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage2);
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage3);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage1, ERROR);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage2, FAILED);
        verify(messageStatusProcessor).processStatusForMessage(cardMessage3, COMPLETED);
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor, createNewCardDummyMessageTypeProcessor);
        verifyNoInteractions(eventAuditor);
    }

    @Test
    void shouldAuditFailedEventWhenAnEventFailedExceptionIsThrown() {
        //Given
        Message cardMessage = aMessageWithMessageTimestamp(LocalDateTime.now().minusHours(1));
        given(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(any(), any()))
                .willReturn(List.of(cardMessage));
        UUID claimId = UUID.randomUUID();
        NewCardEvent event = NewCardEvent.builder().claimId(claimId).build();
        String failureMessage = "Something went badly wrong";
        EventFailedException eventFailedException = new EventFailedException(event, TEST_EXCEPTION, failureMessage);
        given(createNewCardDummyMessageTypeProcessor.processMessage(any())).willThrow(eventFailedException);

        //When
        messageProcessor.processMessagesOfType(REQUEST_NEW_CARD);

        //Then
        verify(createNewCardDummyMessageTypeProcessor).processMessage(cardMessage);
        verify(createNewCardDummyMessageTypeProcessor).processFailedMessage(cardMessage, eventFailedException.getFailureEvent());
        verify(messageStatusProcessor).processStatusForMessage(cardMessage, ERROR);
        verify(eventAuditor).auditFailedEvent(eventFailedException.getFailureEvent());
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor, createNewCardDummyMessageTypeProcessor, eventAuditor);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForMessageWithNoProcessor() {
        LocalDateTime originalTimestamp = LocalDateTime.now().minusHours(1);
        Message cardMessage = aValidMessageWithTypeAndTimestamp(MAKE_PAYMENT, originalTimestamp);
        lenient().when(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(MAKE_PAYMENT, PAGEABLE)).thenReturn(singletonList(cardMessage));
        //When
        IllegalArgumentException thrown = catchThrowableOfType(() -> messageProcessor.processMessagesOfType(MAKE_PAYMENT), IllegalArgumentException.class);
        //Then
        assertThat(thrown).hasMessage("No message type processor found in application context for message type: "
                + "MAKE_PAYMENT, there are 1 message(s) due to be processed");
        verify(messageRepository).findAllMessagesOfTypeWithTimestampBeforeNow(MAKE_PAYMENT, PAGEABLE);
        verify(messageStatusProcessor).updateMessagesToErrorAndIncrementCount(singletonList(cardMessage));
        verifyNoMoreInteractions(messageRepository, messageStatusProcessor);
        verifyNoInteractions(eventAuditor);
    }

    @Test
    void shouldLogMessageProcessResults() {
        Message cardMessage = aValidMessageWithType(REQUEST_NEW_CARD);
        lenient().when(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(REQUEST_NEW_CARD, PAGEABLE)).thenReturn(asList(cardMessage, cardMessage));
        //When
        messageProcessor.processMessagesOfType(REQUEST_NEW_CARD);
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("Processing 2 REQUEST_NEW_CARD message(s)");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(events.get(1).getFormattedMessage()).isEqualTo("Processed 2 REQUEST_NEW_CARD message(s) with status COMPLETED");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void shouldLogNullMessageProcessResult() {
        Message emailMessage = aValidMessageWithType(SEND_EMAIL);
        lenient().when(messageRepository.findAllMessagesOfTypeWithTimestampBeforeNow(SEND_EMAIL, PAGEABLE)).thenReturn(singletonList(emailMessage));
        //When
        messageProcessor.processMessagesOfType(SEND_EMAIL);
        //Then
        List<ILoggingEvent> events = TestAppender.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getFormattedMessage()).isEqualTo("Processing 1 SEND_EMAIL message(s)");
        assertThat(events.get(0).getLevel()).isEqualTo(Level.INFO);
        // use startsWith instead of isEqualTo because the full class name contains mockito text.
        assertThat(events.get(1).getFormattedMessage()).startsWith("Received null message status from MessageTypeProcessor:"
                + " uk.gov.dhsc.htbhf.claimant.message.SendEmailDummyMessageTypeProcessor");
        assertThat(events.get(1).getLevel()).isEqualTo(Level.ERROR);
    }

}
