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
import uk.gov.dhsc.htbhf.claimant.message.context.EmailMessageContext;
import uk.gov.dhsc.htbhf.claimant.message.context.MessageContextLoader;
import uk.gov.dhsc.htbhf.claimant.message.payload.EmailType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.dhsc.htbhf.claimant.message.MessageStatus.COMPLETED;
import static uk.gov.dhsc.htbhf.claimant.message.MessageType.SEND_EMAIL;
import static uk.gov.dhsc.htbhf.claimant.service.audit.FailedEventTestUtils.verifySendEmailEventFailExceptionAndEventAreCorrect;
import static uk.gov.dhsc.htbhf.claimant.testsupport.ClaimTestDataFactory.aValidClaim;
import static uk.gov.dhsc.htbhf.claimant.testsupport.EmailPersonalisationMapTestDataFactory.buildEmailPersonalisation;
import static uk.gov.dhsc.htbhf.claimant.testsupport.MessageTestDataFactory.aValidMessageWithType;
import static uk.gov.dhsc.htbhf.claimant.testsupport.TestConstants.VALID_EMAIL_ADDRESS;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.getLogEvents;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.startRecordingLogsFor;
import static uk.gov.dhsc.htbhf.claimant.utilities.TestLoggingUtilities.stopRecordingLogsFor;

@ExtendWith(MockitoExtension.class)
class EmailMessageProcessorTest {

    private static final String REPLY_TO_ADDRESS_ID = "skjfbnsdkjfbsjhk";

    @Mock
    private NotificationClient client;
    @Mock
    private MessageContextLoader messageContextLoader;

    private EmailMessageProcessor emailMessageProcessor;

    @BeforeEach
    void init() {
        startRecordingLogsFor(EmailMessageProcessor.class);
        emailMessageProcessor = new EmailMessageProcessor(client, messageContextLoader, REPLY_TO_ADDRESS_ID);
    }

    @AfterEach
    void tearDown() {
        stopRecordingLogsFor(EmailMessageProcessor.class);
    }

    @Test
    void shouldSendMessage() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        String templateId = "12334546";
        Map<String, Object> emailPersonalisation = buildEmailPersonalisation();
        EmailMessageContext context = EmailMessageContext.builder()
                .claim(claim)
                .templateId(templateId)
                .emailPersonalisation(emailPersonalisation)
                .emailType(EmailType.INSTANT_SUCCESS)
                .build();
        given(messageContextLoader.loadEmailMessageContext(any())).willReturn(context);
        given(client.sendEmail(any(), any(), any(), any(), any())).willReturn(mock(SendEmailResponse.class));
        Message message = aValidMessageWithType(SEND_EMAIL);

        //When
        MessageStatus status = emailMessageProcessor.processMessage(message);

        //Then
        assertThat(status).isEqualTo(COMPLETED);
        verify(messageContextLoader).loadEmailMessageContext(message);
        verify(client).sendEmail(eq(templateId), eq(VALID_EMAIL_ADDRESS), eq(emailPersonalisation), any(String.class), eq(REPLY_TO_ADDRESS_ID));
    }

    @Test
    void shouldNotLogEmailBodyOrSubject() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        String templateId = "12334546";
        Map<String, Object> emailPersonalisation = buildEmailPersonalisation();
        EmailMessageContext context = EmailMessageContext.builder()
                .claim(claim)
                .templateId(templateId)
                .emailPersonalisation(emailPersonalisation)
                .emailType(EmailType.INSTANT_SUCCESS)
                .build();
        given(messageContextLoader.loadEmailMessageContext(any())).willReturn(context);
        SendEmailResponse response = mock(SendEmailResponse.class);
        given(client.sendEmail(any(), any(), any(), any(), any())).willReturn(response);
        String emailBody = "[the email body]";
        String emailSubject = "[the email subject]";
        UUID notificationId = UUID.randomUUID();
        lenient().when(response.toString()).thenReturn("Text that includes " + emailBody + " and " + emailSubject);
        lenient().when(response.getBody()).thenReturn(emailBody);
        lenient().when(response.getSubject()).thenReturn(emailSubject);
        when(response.getNotificationId()).thenReturn(notificationId);
        Message message = aValidMessageWithType(SEND_EMAIL);

        //When
        emailMessageProcessor.processMessage(message);

        //Then
        List<ILoggingEvent> events = getLogEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getFormattedMessage()).doesNotContain(emailBody);
        assertThat(events.get(0).getFormattedMessage()).doesNotContain(emailSubject);
        assertThat(events.get(0).getFormattedMessage()).contains(notificationId.toString());
    }

    @Test
    void shouldThrowFailedEventExceptionWhenSendMessageFails() throws NotificationClientException {
        //Given
        Claim claim = aValidClaim();
        String templateId = "12334546";
        Map<String, Object> emailPersonalisation = buildEmailPersonalisation();
        EmailMessageContext context = EmailMessageContext.builder()
                .claim(claim)
                .templateId(templateId)
                .emailPersonalisation(emailPersonalisation)
                .emailType(EmailType.INSTANT_SUCCESS)
                .build();
        given(messageContextLoader.loadEmailMessageContext(any())).willReturn(context);
        Message message = aValidMessageWithType(SEND_EMAIL);
        NotificationClientException testException = new NotificationClientException("Test exception from message send");
        given(client.sendEmail(anyString(), anyString(), any(), anyString(), anyString()))
                .willThrow(testException);

        //When
        EventFailedException thrown = catchThrowableOfType(() -> emailMessageProcessor.processMessage(message), EventFailedException.class);

        //Then
        verifySendEmailEventFailExceptionAndEventAreCorrect(claim, testException, thrown, templateId);
        verify(messageContextLoader).loadEmailMessageContext(message);
        verify(client).sendEmail(eq(templateId), eq(VALID_EMAIL_ADDRESS), eq(emailPersonalisation), any(String.class), eq(REPLY_TO_ADDRESS_ID));
    }

}
