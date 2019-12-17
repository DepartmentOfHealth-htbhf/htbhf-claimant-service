package uk.gov.dhsc.htbhf.claimant.message.processor;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dhsc.htbhf.claimant.entity.Claim;
import uk.gov.dhsc.htbhf.claimant.entity.Message;
import uk.gov.dhsc.htbhf.claimant.exception.EventFailedException;
import uk.gov.dhsc.htbhf.claimant.message.MessageStatus;
import uk.gov.dhsc.htbhf.claimant.message.context.LetterMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendLetterResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_LETTER;
import static uk.gov.dhsc.htbhf.claimant.message.payload.LetterType.UPDATE_YOUR_ADDRESS;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifySendLetterEventFailExceptionAndEventAreCorrect;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.PersonalisationMapTestDataFactory.buildLetterPersonalisation;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.getLogEvents;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.startRecordingLogsFor;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.stopRecordingLogsFor;

@ExtendWith(MockitoExtension.class)
class LetterMessageProcessorTest {

    @Mock
    private NotificationClient client;
    @Mock
    private MessageContextLoader messageContextLoader;
    
    private LetterMessageProcessor letterMessageProcessor;
    public static final String LETTER_BODY = "[the letter body]";
    public static final String LETTER_SUBJECT = "[the letter subject]";

    @BeforeEach
    void init() {
        letterMessageProcessor = new LetterMessageProcessor(client, messageContextLoader);
        startRecordingLogsFor(LetterMessageProcessor.class);
    }

    @AfterEach
    void tearDown() {
        stopRecordingLogsFor(LetterMessageProcessor.class);
    }

    @Test
    void shouldSendMessage() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        Map<String, Object> personalisation = buildLetterPersonalisation();
        LetterMessageContext context = LetterMessageContext.builder()
                .claim(claim)
                .personalisation(personalisation)
                .letterType(UPDATE_YOUR_ADDRESS)
                .build();
        given(messageContextLoader.loadLetterMessageContext(any())).willReturn(context);
        given(client.sendLetter(any(), any(), any())).willReturn(mock(SendLetterResponse.class));
        Message message = aValidMessageWithType(SEND_LETTER);

        //When
        MessageStatus status = letterMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadLetterMessageContext(message);
        verify(client).sendLetter(eq(UPDATE_YOUR_ADDRESS.getTemplateId()), eq(personalisation), any(String.class));
    }

    @Test
    void shouldNotLogLetterBodyOrSubject() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        Map<String, Object> personalisation = buildLetterPersonalisation();
        LetterMessageContext context = LetterMessageContext.builder()
                .claim(claim)
                .personalisation(personalisation)
                .letterType(UPDATE_YOUR_ADDRESS)
                .build();
        given(messageContextLoader.loadLetterMessageContext(any())).willReturn(context);
        UUID notificationId = UUID.randomUUID();
        givenSendLetterResponseWillIncludeTheLetterSubjectAndBody(notificationId);
        Message message = aValidMessageWithType(SEND_LETTER);

        //When
        letterMessageProcessor.processMessage(message);

        //Then
        List<ILoggingEvent> events = getLogEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).doesNotContain(LETTER_BODY);
        assertThat(events.get(0).getFormattedMessage()).doesNotContain(LETTER_SUBJECT);
        assertThat(events.get(0).getFormattedMessage()).contains(notificationId.toString());
    }

    private void givenSendLetterResponseWillIncludeTheLetterSubjectAndBody(UUID notificationId) throws NotificationClientException {
        SendLetterResponse response = mock(SendLetterResponse.class);
        given(client.sendLetter(any(), any(), any())).willReturn(response);
        lenient().when(response.toString()).thenReturn("Text that includes " + LETTER_BODY + " and " + LETTER_SUBJECT);
        lenient().when(response.getBody()).thenReturn(LETTER_BODY);
        lenient().when(response.getSubject()).thenReturn(LETTER_SUBJECT);
        when(response.getNotificationId()).thenReturn(notificationId);
    }

    @Test
    void shouldThrowFailedEventExceptionWhenSendMessageFails() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        Map<String, Object> personalisation = buildLetterPersonalisation();
        LetterMessageContext context = LetterMessageContext.builder()
                .claim(claim)
                .personalisation(personalisation)
                .letterType(UPDATE_YOUR_ADDRESS)
                .build();
        given(messageContextLoader.loadLetterMessageContext(any())).willReturn(context);
        Message message = aValidMessageWithType(SEND_LETTER);
        NotificationClientException testException = new NotificationClientException("Test exception from message send");
        given(client.sendLetter(anyString(), any(), any())).willThrow(testException);

        //When
        EventFailedException thrown = catchThrowableOfType(() -> letterMessageProcessor.processMessage(message), EventFailedException.class);

        //Then
        verifySendLetterEventFailExceptionAndEventAreCorrect(claim, testException, thrown, UPDATE_YOUR_ADDRESS.getTemplateId());
        verify(messageContextLoader).loadLetterMessageContext(message);
        verify(client).sendLetter(eq(UPDATE_YOUR_ADDRESS.getTemplateId()), eq(personalisation), any(String.class));
    }

}
